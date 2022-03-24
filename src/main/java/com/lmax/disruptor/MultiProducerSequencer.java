/*
 * Copyright 2011 LMAX Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lmax.disruptor;

import com.lmax.disruptor.util.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.concurrent.locks.LockSupport;


/**
 * Coordinator for claiming sequences for access to a data structure while tracking dependent {@link Sequence}s.
 * Suitable for use for sequencing across multiple publisher threads.
 * 协调器，用于在跟踪依赖序列的同时声明访问数据结构的序列。适用于跨多个发布者线程排序
 * <p>Note on {@link Sequencer#getCursor()}:  With this sequencer the cursor value is updated after the call
 * to {@link Sequencer#next()}, to determine the highest available sequence that can be read, then
 * {@link Sequencer#getHighestPublishedSequence(long, long)} should be used.
 */
public final class MultiProducerSequencer extends AbstractSequencer
{
    private static final VarHandle AVAILABLE_ARRAY = MethodHandles.arrayElementVarHandle(int[].class);

    private final Sequence gatingSequenceCache = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);

    // availableBuffer tracks the state of each ringbuffer slot 用于跟踪ringbuffer中每个插槽的状态
    // see below for more details on the approach
    private final int[] availableBuffer;
    //索引掩码
    private final int indexMask;
    private final int indexShift;

    /**
     * Construct a Sequencer with the selected wait strategy and buffer size.
     *
     * @param bufferSize   the size of the buffer that this will sequence over.
     * @param waitStrategy for those waiting on sequences.
     */
    public MultiProducerSequencer(final int bufferSize, final WaitStrategy waitStrategy)
    {
        super(bufferSize, waitStrategy);
        availableBuffer = new int[bufferSize];
        //填充数组为-1
        Arrays.fill(availableBuffer, -1);

        indexMask = bufferSize - 1;
        indexShift = Util.log2(bufferSize);
    }

    /**
     * @see Sequencer#hasAvailableCapacity(int)
     */
    @Override
    public boolean hasAvailableCapacity(final int requiredCapacity)
    {
        return hasAvailableCapacity(gatingSequences, requiredCapacity, cursor.get());
    }

    /**
     * 是否有足够容量
     * @param gatingSequences 门闩序列数组
     * @param requiredCapacity 申请的容量
     * @param cursorValue 游标值，指针指向值
     * @return
     */
    private boolean hasAvailableCapacity(final Sequence[] gatingSequences, final int requiredCapacity, final long cursorValue)
    {
        //当前指针+申请容量-环形数组长度
        long wrapPoint = (cursorValue + requiredCapacity) - bufferSize;
        //缓存门闩序列值
        long cachedGatingSequence = gatingSequenceCache.get();

        if (wrapPoint > cachedGatingSequence || cachedGatingSequence > cursorValue)
        {
            //获取最小消费序列值
            long minSequence = Util.getMinimumSequence(gatingSequences, cursorValue);
            //使用最小消费序列值设置缓存门闩序列值
            gatingSequenceCache.set(minSequence);
            //说明超过环形数组一圈，没有空间
            if (wrapPoint > minSequence)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * @see Sequencer#claim(long)
     */
    @Override
    public void claim(final long sequence)
    {
        cursor.set(sequence);
    }

    /**
     * @see Sequencer#next()
     */
    @Override
    public long next()
    {
        return next(1);
    }

    /**
     * @see Sequencer#next(int)
     */
    @Override
    public long next(final int n)
    {
        if (n < 1 || n > bufferSize)
        {
            throw new IllegalArgumentException("n must be > 0 and < bufferSize");
        }
        //获取游标+1
        long current = cursor.getAndAdd(n);
        //下一个 序列
        long nextSequence = current + n;
        long wrapPoint = nextSequence - bufferSize;
        //缓存的门闩序列值
        long cachedGatingSequence = gatingSequenceCache.get();
        //nextSequence是否超过cachedGatingSequence一圈 或
        if (wrapPoint > cachedGatingSequence || cachedGatingSequence > current)
        {
            long gatingSequence;
            //门闩序列中的最小值
            while (wrapPoint > (gatingSequence = Util.getMinimumSequence(gatingSequences, current)))
            {
                //阻塞等待
                LockSupport.parkNanos(1L); // TODO, should we spin based on the wait strategy?
            }
            //门闩序列中的最小值缓存起来
            gatingSequenceCache.set(gatingSequence);
        }

        return nextSequence;
    }

    /**
     * @see Sequencer#tryNext()
     */
    @Override
    public long tryNext() throws InsufficientCapacityException
    {
        return tryNext(1);
    }

    /**
     * @see Sequencer#tryNext(int)
     */
    @Override
    public long tryNext(final int n) throws InsufficientCapacityException
    {
        if (n < 1)
        {
            throw new IllegalArgumentException("n must be > 0");
        }

        long current;
        long next;

        do
        {
            current = cursor.get();
            next = current + n;
            //不足以申请n个容量
            if (!hasAvailableCapacity(gatingSequences, n, current))
            {
                //抛异常
                throw InsufficientCapacityException.INSTANCE;
            }
        }
        while (!cursor.compareAndSet(current, next));

        return next;
    }

    /**
     * @see Sequencer#remainingCapacity()
     */
    @Override
    public long remainingCapacity()
    {
        long consumed = Util.getMinimumSequence(gatingSequences, cursor.get());
        long produced = cursor.get();
        return getBufferSize() - (produced - consumed);
    }

    /**
     * @see Sequencer#publish(long)
     */
    @Override
    public void publish(final long sequence)
    {
        setAvailable(sequence);
        waitStrategy.signalAllWhenBlocking();
    }

    /**
     * @see Sequencer#publish(long, long)
     */
    @Override
    public void publish(final long lo, final long hi)
    {
        for (long l = lo; l <= hi; l++)
        {
            setAvailable(l);
        }
        waitStrategy.signalAllWhenBlocking();
    }

    /**
     * 设置环形数组可用状态
     * @param sequence
     */
    private void setAvailable(final long sequence)
    {
        setAvailableBufferValue(calculateIndex(sequence), calculateAvailabilityFlag(sequence));
    }

    /**
     * 设置环形数组当前索引是否可用标识
     * @param index
     * @param flag
     */
    private void setAvailableBufferValue(final int index, final int flag)
    {
        //将变量的值设置为newValue，并确保在此访问之后之前的加载和存储不会重新排序
        AVAILABLE_ARRAY.setRelease(availableBuffer, index, flag);
    }

    /**
     * @see Sequencer#isAvailable(long)
     */
    @Override
    public boolean isAvailable(final long sequence)
    {
        //计算索引
        int index = calculateIndex(sequence);
        //计算可用标志
        int flag = calculateAvailabilityFlag(sequence);
        //获取变量的值，并确保在此访问之前不会对后续加载和存储重新排序,判断是否可用
        return (int) AVAILABLE_ARRAY.getAcquire(availableBuffer, index) == flag;
    }

    @Override
    public long getHighestPublishedSequence(final long lowerBound, final long availableSequence)
    {
        for (long sequence = lowerBound; sequence <= availableSequence; sequence++)
        {
            if (!isAvailable(sequence))
            {
                return sequence - 1;
            }
        }

        return availableSequence;
    }

    /**
     * 计算可用性标志
     * @param sequence
     * @return
     */
    private int calculateAvailabilityFlag(final long sequence)
    {
        return (int) (sequence >>> indexShift);
    }

    /**
     * 计算索引
     * @param sequence
     * @return
     */
    private int calculateIndex(final long sequence)
    {
        return ((int) sequence) & indexMask;
    }

    @Override
    public String toString()
    {
        return "MultiProducerSequencer{" +
                "bufferSize=" + bufferSize +
                ", waitStrategy=" + waitStrategy +
                ", cursor=" + cursor +
                ", gatingSequences=" + Arrays.toString(gatingSequences) +
                '}';
    }
}
