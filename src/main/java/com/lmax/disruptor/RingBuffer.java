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


import com.lmax.disruptor.dsl.ProducerType;

/**
 * 环形数组填充
 */
abstract class RingBufferPad
{
    protected byte
        p10, p11, p12, p13, p14, p15, p16, p17,
        p20, p21, p22, p23, p24, p25, p26, p27,
        p30, p31, p32, p33, p34, p35, p36, p37,
        p40, p41, p42, p43, p44, p45, p46, p47,
        p50, p51, p52, p53, p54, p55, p56, p57,
        p60, p61, p62, p63, p64, p65, p66, p67,
        p70, p71, p72, p73, p74, p75, p76, p77;
}

/**
 * 环形数组字段
 * @param <E>
 */
abstract class RingBufferFields<E> extends RingBufferPad
{
    private static final int BUFFER_PAD = 32;

    //索引掩码
    private final long indexMask;
    //数组
    private final E[] entries;
    //元素个数
    protected final int bufferSize;
    protected final Sequencer sequencer;

    @SuppressWarnings("unchecked")
    RingBufferFields(
        final EventFactory<E> eventFactory,
        final Sequencer sequencer)
    {
        this.sequencer = sequencer;
        this.bufferSize = sequencer.getBufferSize();

        if (bufferSize < 1)
        {
            throw new IllegalArgumentException("bufferSize must not be less than 1");
        }
        //环形数组长度必须是2的次幂
        if (Integer.bitCount(bufferSize) != 1)
        {
            throw new IllegalArgumentException("bufferSize must be a power of 2");
        }
        //索引掩码
        this.indexMask = bufferSize - 1;
        //初始化数组，数组长度为元素个数+2*32
        this.entries = (E[]) new Object[sequencer.getBufferSize() + 2 * BUFFER_PAD];
        //填充数组
        fill(eventFactory);
    }

    /**
     * 填充数组
     * @param eventFactory
     */
    private void fill(final EventFactory<E> eventFactory)
    {
        for (int i = 0; i < bufferSize; i++)
        {
            //使用事件工厂新实例填充数组
            entries[BUFFER_PAD + i] = eventFactory.newInstance();
        }
    }

    /**
     * 获取数组中的元素
     * @param sequence
     * @return
     */
    protected final E elementAt(final long sequence)
    {
        //序列和索引掩码与操作，相当于序列对索引掩码取模操作，获取数组中的下标
        return entries[BUFFER_PAD + (int) (sequence & indexMask)];
    }
}

/**
 * Ring based store of reusable entries containing the data representing
 * an event being exchanged between event producer and {@link EventProcessor}s.
 * 基于环的可重用条目存储，其中包含表示事件生成器和事件处理器之间交换的事件的数据
 * @param <E> implementation storing the data for sharing during exchange or parallel coordination of an event.
 */
public final class RingBuffer<E> extends RingBufferFields<E> implements Cursored, EventSequencer<E>, EventSink<E>
{
    /**
     * The initial cursor value
     * 初始化游标值
     */
    public static final long INITIAL_CURSOR_VALUE = Sequence.INITIAL_VALUE;
    protected byte
        p10, p11, p12, p13, p14, p15, p16, p17,
        p20, p21, p22, p23, p24, p25, p26, p27,
        p30, p31, p32, p33, p34, p35, p36, p37,
        p40, p41, p42, p43, p44, p45, p46, p47,
        p50, p51, p52, p53, p54, p55, p56, p57,
        p60, p61, p62, p63, p64, p65, p66, p67,
        p70, p71, p72, p73, p74, p75, p76, p77;

    /**
     * Construct a RingBuffer with the full option set.
     * 使用事件工厂和序列构建RingBuffer
     * @param eventFactory to newInstance entries for filling the RingBuffer
     * @param sequencer    sequencer to handle the ordering of events moving through the RingBuffer.
     * @throws IllegalArgumentException if bufferSize is less than 1 or not a power of 2
     */
    RingBuffer(
        final EventFactory<E> eventFactory,
        final Sequencer sequencer)
    {
        super(eventFactory, sequencer);
    }

    /**
     * Create a new multiple producer RingBuffer with the specified wait strategy.
     * 使用指定的等待策略创建新的多生产者环形缓冲区
     * @param <E> Class of the event stored in the ring buffer.
     * @param factory      used to create the events within the ring buffer.
     * @param bufferSize   number of elements to create within the ring buffer.
     * @param waitStrategy used to determine how to wait for new elements to become available.
     * @return a constructed ring buffer.
     * @throws IllegalArgumentException if bufferSize is less than 1 or not a power of 2
     * @see MultiProducerSequencer
     */
    public static <E> RingBuffer<E> createMultiProducer(
        final EventFactory<E> factory,
        final int bufferSize,
        final WaitStrategy waitStrategy)
    {
        MultiProducerSequencer sequencer = new MultiProducerSequencer(bufferSize, waitStrategy);

        return new RingBuffer<>(factory, sequencer);
    }

    /**
     * Create a new multiple producer RingBuffer using the default wait strategy  {@link BlockingWaitStrategy}.
     * 使用默认的等待策略BlockingWaitStrategy创建一个新的多生产者环形缓冲区
     * @param <E> Class of the event stored in the ring buffer.
     * @param factory    used to create the events within the ring buffer.
     * @param bufferSize number of elements to create within the ring buffer.
     * @return a constructed ring buffer.
     * @throws IllegalArgumentException if <code>bufferSize</code> is less than 1 or not a power of 2
     * @see MultiProducerSequencer
     */
    public static <E> RingBuffer<E> createMultiProducer(final EventFactory<E> factory, final int bufferSize)
    {
        return createMultiProducer(factory, bufferSize, new BlockingWaitStrategy());
    }

    /**
     * Create a new single producer RingBuffer with the specified wait strategy.
     * 使用指定的等待策略创建新的单生产者环形缓冲区
     * @param <E> Class of the event stored in the ring buffer.
     * @param factory      used to create the events within the ring buffer.
     * @param bufferSize   number of elements to create within the ring buffer.
     * @param waitStrategy used to determine how to wait for new elements to become available.
     * @return a constructed ring buffer.
     * @throws IllegalArgumentException if bufferSize is less than 1 or not a power of 2
     * @see SingleProducerSequencer
     */
    public static <E> RingBuffer<E> createSingleProducer(
        final EventFactory<E> factory,
        final int bufferSize,
        final WaitStrategy waitStrategy)
    {
        SingleProducerSequencer sequencer = new SingleProducerSequencer(bufferSize, waitStrategy);

        return new RingBuffer<>(factory, sequencer);
    }

    /**
     * Create a new single producer RingBuffer using the default wait strategy  {@link BlockingWaitStrategy}.
     * 使用默认的等待策略BlockingWaitStrategy创建一个新的单生产者环形缓冲区
     * @param <E> Class of the event stored in the ring buffer.
     * @param factory    used to create the events within the ring buffer.
     * @param bufferSize number of elements to create within the ring buffer.
     * @return a constructed ring buffer.
     * @throws IllegalArgumentException if <code>bufferSize</code> is less than 1 or not a power of 2
     * @see MultiProducerSequencer
     */
    public static <E> RingBuffer<E> createSingleProducer(final EventFactory<E> factory, final int bufferSize)
    {
        return createSingleProducer(factory, bufferSize, new BlockingWaitStrategy());
    }

    /**
     * Create a new Ring Buffer with the specified producer type (SINGLE or MULTI)
     * 使用指定的生产者类型（单个或多个）创建新的环形缓冲区
     * @param <E> Class of the event stored in the ring buffer.
     * @param producerType producer type to use {@link ProducerType}.
     * @param factory      used to create events within the ring buffer.
     * @param bufferSize   number of elements to create within the ring buffer.
     * @param waitStrategy used to determine how to wait for new elements to become available.
     * @return a constructed ring buffer.
     * @throws IllegalArgumentException if bufferSize is less than 1 or not a power of 2
     */
    public static <E> RingBuffer<E> create(
        final ProducerType producerType,
        final EventFactory<E> factory,
        final int bufferSize,
        final WaitStrategy waitStrategy)
    {
        switch (producerType)
        {
            case SINGLE:
                return createSingleProducer(factory, bufferSize, waitStrategy);
            case MULTI:
                return createMultiProducer(factory, bufferSize, waitStrategy);
            default:
                throw new IllegalStateException(producerType.toString());
        }
    }

    /**
     * <p>Get the event for a given sequence in the RingBuffer.</p>
     * 获取RingBuffer中给定序列的事件
     * <p>This call has 2 uses.  Firstly use this call when publishing to a ring buffer.
     * After calling {@link RingBuffer#next()} use this call to get hold of the
     * preallocated event to fill with data before calling {@link RingBuffer#publish(long)}.</p>
     *
     * <p>Secondly use this call when consuming data from the ring buffer.  After calling
     * {@link SequenceBarrier#waitFor(long)} call this method with any value greater than
     * that your current consumer sequence and less than or equal to the value returned from
     * the {@link SequenceBarrier#waitFor(long)} method.</p>
     *
     * @param sequence for the event
     * @return the event for the given sequence
     */
    @Override
    public E get(final long sequence)
    {
        return elementAt(sequence);
    }

    /**
     * Increment and return the next sequence for the ring buffer.  Calls of this 递增并返回环形缓冲区的下一个序列。此方法的调用应确保始终在之后发布序列
     * method should ensure that they always publish the sequence afterward.  E.g.
     * <pre>
     * long sequence = ringBuffer.next();
     * try {
     *     Event e = ringBuffer.get(sequence);
     *     // Do some work with the event.
     * } finally {
     *     ringBuffer.publish(sequence);
     * }
     * </pre>
     *
     * @return The next sequence to publish to.
     * @see RingBuffer#publish(long)
     * @see RingBuffer#get(long)
     */
    @Override
    public long next()
    {
        return sequencer.next();
    }

    /**
     * The same functionality as {@link RingBuffer#next()}, but allows the caller to claim
     * the next n sequences.
     *
     * @param n number of slots to claim
     * @return sequence number of the highest slot claimed
     * @see Sequencer#next(int)
     */
    @Override
    public long next(final int n)
    {
        return sequencer.next(n);
    }

    /**
     * <p>Increment and return the next sequence for the ring buffer.  Calls of this 递增并返回环形缓冲区的下一个序列。此方法的调用应确保始终在之后发布序列
     * method should ensure that they always publish the sequence afterward.  E.g.</p>
     * <pre>
     * long sequence = ringBuffer.next();
     * try {
     *     Event e = ringBuffer.get(sequence);
     *     // Do some work with the event.
     * } finally {
     *     ringBuffer.publish(sequence);
     * }
     * </pre>
     * <p>This method will not block if there is not space available in the ring
     * buffer, instead it will throw an {@link InsufficientCapacityException}.</p>
     *
     * @return The next sequence to publish to.
     * @throws InsufficientCapacityException if the necessary space in the ring buffer is not available
     * @see RingBuffer#publish(long)
     * @see RingBuffer#get(long)
     */
    @Override
    public long tryNext() throws InsufficientCapacityException
    {
        return sequencer.tryNext();
    }

    /**
     * The same functionality as {@link RingBuffer#tryNext()}, but allows the caller to attempt
     * to claim the next n sequences.
     *
     * @param n number of slots to claim
     * @return sequence number of the highest slot claimed
     * @throws InsufficientCapacityException if the necessary space in the ring buffer is not available
     */
    @Override
    public long tryNext(final int n) throws InsufficientCapacityException
    {
        return sequencer.tryNext(n);
    }

    /**
     * Resets the cursor to a specific value.  This can be applied at any time, but it is worth noting
     * that it can cause a data race and should only be used in controlled circumstances.  E.g. during
     * initialisation.
     * 将光标重置为特定值。这可以在任何时候应用，但值得注意的是，它可能会导致数据竞争，只应在受控环境下使用。例如，在初始化过程中
     * @param sequence The sequence to reset too.
     * @throws IllegalStateException If any gating sequences have already been specified.
     */
    @Deprecated
    public void resetTo(final long sequence)
    {
        sequencer.claim(sequence);
        sequencer.publish(sequence);
    }

    /**
     * Sets the cursor to a specific sequence and returns the preallocated entry that is stored there.  This
     * can cause a data race and should only be done in controlled circumstances, e.g. during initialisation.
     * 将光标设置为特定序列，并返回存储在其中的预分配项。这可能会导致数据竞争，只能在受控情况下进行，例如在初始化期间。
     * @param sequence The sequence to claim.
     * @return The preallocated event.
     */
    public E claimAndGetPreallocated(final long sequence)
    {
        sequencer.claim(sequence);
        return get(sequence);
    }

    /**
     * Determines if the event for a given sequence is currently available.
     * 确定给定序列的事件当前是否可用
     * <p>Note that this does not guarantee that event will still be available
     * on the next interaction with the RingBuffer. For example, it is not
     * necessarily safe to write code like this:
     *
     * <pre>{@code
     * if (ringBuffer.isAvailable(sequence))
     * {
     *     final E e = ringBuffer.get(sequence);
     *     // ...do something with e
     * }
     * }</pre>
     *
     * <p>because there is a race between the reading thread and the writing thread.
     *
     * <p>This method will also return false when querying for sequences that are
     * behind the ring buffer's wrap point.
     *
     * @param sequence The sequence to identify the entry.
     * @return If the event published with the given sequence number is currently available.
     */
    public boolean isAvailable(final long sequence)
    {
        return sequencer.isAvailable(sequence);
    }

    /**
     * Add the specified gating sequences to this instance of the Disruptor.  They will
     * safely and atomically added to the list of gating sequences.
     * 将指定的选通序列添加到此中断器实例。它们将安全且原子化地添加到选通序列列表中
     * @param gatingSequences The sequences to add.
     */
    public void addGatingSequences(final Sequence... gatingSequences)
    {
        sequencer.addGatingSequences(gatingSequences);
    }

    /**
     * Get the minimum sequence value from all of the gating sequences
     * added to this ringBuffer.
     * 从添加到此ringBuffer的所有选通序列中获取最小序列值
     * @return The minimum gating sequence or the cursor sequence if
     * no sequences have been added.
     */
    public long getMinimumGatingSequence()
    {
        return sequencer.getMinimumSequence();
    }

    /**
     * Remove the specified sequence from this ringBuffer.
     * 从此ringBuffer中删除指定的序列
     * @param sequence to be removed.
     * @return <code>true</code> if this sequence was found, <code>false</code> otherwise.
     */
    public boolean removeGatingSequence(final Sequence sequence)
    {
        return sequencer.removeGatingSequence(sequence);
    }

    /**
     * Create a new SequenceBarrier to be used by an EventProcessor to track which messages
     * are available to be read from the ring buffer given a list of sequences to track.
     * 创建一个新的SequenceBarrier，供EventProcessor使用，以便在给定要跟踪的序列列表的情况下，跟踪哪些消息可从环形缓冲区读取
     * @param sequencesToTrack the additional sequences to track
     * @return A sequence barrier that will track the specified sequences.
     * @see SequenceBarrier
     */
    public SequenceBarrier newBarrier(final Sequence... sequencesToTrack)
    {
        return sequencer.newBarrier(sequencesToTrack);
    }

    /**
     * Creates an event poller for this ring buffer gated on the supplied sequences.
     * 在提供的序列上为该环形缓冲区创建一个事件轮询器
     * @param gatingSequences to be gated on.
     * @return A poller that will gate on this ring buffer and the supplied sequences.
     */
    public EventPoller<E> newPoller(final Sequence... gatingSequences)
    {
        return sequencer.newPoller(this, gatingSequences);
    }

    /**
     * Get the current cursor value for the ring buffer.  The actual value received
     * will depend on the type of {@link Sequencer} that is being used.
     * 获取环形缓冲区的当前光标值。收到的实际值将取决于正在使用的定序器的类型
     * @see MultiProducerSequencer
     * @see SingleProducerSequencer
     */
    @Override
    public long getCursor()
    {
        return sequencer.getCursor();
    }

    /**
     * The size of the buffer.
     * 缓冲区的大小
     * @return size of buffer
     */
    @Override
    public int getBufferSize()
    {
        return bufferSize;
    }

    /**
     * Given specified <code>requiredCapacity</code> determines if that amount of space
     * is available.  Note, you can not assume that if this method returns <code>true</code>
     * that a call to {@link RingBuffer#next()} will not block.  Especially true if this
     * ring buffer is set up to handle multiple producers.
     * 给定指定的所需容量决定是否有足够的空间。注意，您不能假设如果此方法返回true，则对next（）的调用不会被阻止。尤其是当这个环形缓冲区被设置为处理多个生产者时
     * @param requiredCapacity The capacity to check for.
     * @return <code>true</code> If the specified <code>requiredCapacity</code> is available
     * <code>false</code> if not.
     */
    @Override
    public boolean hasAvailableCapacity(final int requiredCapacity)
    {
        return sequencer.hasAvailableCapacity(requiredCapacity);
    }


    /**
     * @see com.lmax.disruptor.EventSink#publishEvent(com.lmax.disruptor.EventTranslator)
     * 发布事件
     */
    @Override
    public void publishEvent(final EventTranslator<E> translator)
    {
        final long sequence = sequencer.next();
        translateAndPublish(translator, sequence);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvent(com.lmax.disruptor.EventTranslator)
     * 尝试发布事件
     */
    @Override
    public boolean tryPublishEvent(final EventTranslator<E> translator)
    {
        try
        {
            final long sequence = sequencer.tryNext();
            translateAndPublish(translator, sequence);
            return true;
        }
        catch (InsufficientCapacityException e)
        {
            return false;
        }
    }

    /**
     * @see com.lmax.disruptor.EventSink#publishEvent(com.lmax.disruptor.EventTranslatorOneArg, Object)
     * com.lmax.disruptor.EventSink#publishEvent(com.lmax.disruptor.EventTranslatorOneArg, A)
     */
    @Override
    public <A> void publishEvent(final EventTranslatorOneArg<E, A> translator, final A arg0)
    {
        final long sequence = sequencer.next();
        translateAndPublish(translator, sequence, arg0);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvent(com.lmax.disruptor.EventTranslatorOneArg, Object)
     * com.lmax.disruptor.EventSink#tryPublishEvent(com.lmax.disruptor.EventTranslatorOneArg, A)
     */
    @Override
    public <A> boolean tryPublishEvent(final EventTranslatorOneArg<E, A> translator, final A arg0)
    {
        try
        {
            final long sequence = sequencer.tryNext();
            translateAndPublish(translator, sequence, arg0);
            return true;
        }
        catch (InsufficientCapacityException e)
        {
            return false;
        }
    }

    /**
     * @see com.lmax.disruptor.EventSink#publishEvent(com.lmax.disruptor.EventTranslatorTwoArg, Object, Object)
     * com.lmax.disruptor.EventSink#publishEvent(com.lmax.disruptor.EventTranslatorTwoArg, A, B)
     */
    @Override
    public <A, B> void publishEvent(final EventTranslatorTwoArg<E, A, B> translator, final A arg0, final B arg1)
    {
        final long sequence = sequencer.next();
        translateAndPublish(translator, sequence, arg0, arg1);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvent(com.lmax.disruptor.EventTranslatorTwoArg, Object, Object)
     * com.lmax.disruptor.EventSink#tryPublishEvent(com.lmax.disruptor.EventTranslatorTwoArg, A, B)
     */
    @Override
    public <A, B> boolean tryPublishEvent(final EventTranslatorTwoArg<E, A, B> translator, final A arg0, final B arg1)
    {
        try
        {
            final long sequence = sequencer.tryNext();
            translateAndPublish(translator, sequence, arg0, arg1);
            return true;
        }
        catch (InsufficientCapacityException e)
        {
            return false;
        }
    }

    /**
     * @see com.lmax.disruptor.EventSink#publishEvent(com.lmax.disruptor.EventTranslatorThreeArg, Object, Object, Object)
     * com.lmax.disruptor.EventSink#publishEvent(com.lmax.disruptor.EventTranslatorThreeArg, A, B, C)
     */
    @Override
    public <A, B, C> void publishEvent(final EventTranslatorThreeArg<E, A, B, C> translator, final A arg0, final B arg1, final C arg2)
    {
        final long sequence = sequencer.next();
        translateAndPublish(translator, sequence, arg0, arg1, arg2);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvent(com.lmax.disruptor.EventTranslatorThreeArg, Object, Object, Object)
     * com.lmax.disruptor.EventSink#tryPublishEvent(com.lmax.disruptor.EventTranslatorThreeArg, A, B, C)
     */
    @Override
    public <A, B, C> boolean tryPublishEvent(final EventTranslatorThreeArg<E, A, B, C> translator, final A arg0, final B arg1, final C arg2)
    {
        try
        {
            final long sequence = sequencer.tryNext();
            translateAndPublish(translator, sequence, arg0, arg1, arg2);
            return true;
        }
        catch (InsufficientCapacityException e)
        {
            return false;
        }
    }

    /**
     * @see com.lmax.disruptor.EventSink#publishEvent(com.lmax.disruptor.EventTranslatorVararg, java.lang.Object...)
     */
    @Override
    public void publishEvent(final EventTranslatorVararg<E> translator, final Object... args)
    {
        final long sequence = sequencer.next();
        translateAndPublish(translator, sequence, args);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvent(com.lmax.disruptor.EventTranslatorVararg, java.lang.Object...)
     */
    @Override
    public boolean tryPublishEvent(final EventTranslatorVararg<E> translator, final Object... args)
    {
        try
        {
            final long sequence = sequencer.tryNext();
            translateAndPublish(translator, sequence, args);
            return true;
        }
        catch (InsufficientCapacityException e)
        {
            return false;
        }
    }


    /**
     * @see com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslator[])
     */
    @Override
    public void publishEvents(final EventTranslator<E>[] translators)
    {
        publishEvents(translators, 0, translators.length);
    }

    /**
     * @see com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslator[], int, int)
     */
    @Override
    public void publishEvents(final EventTranslator<E>[] translators, final int batchStartsAt, final int batchSize)
    {
        checkBounds(translators, batchStartsAt, batchSize);
        final long finalSequence = sequencer.next(batchSize);
        translateAndPublishBatch(translators, batchStartsAt, batchSize, finalSequence);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslator[])
     */
    @Override
    public boolean tryPublishEvents(final EventTranslator<E>[] translators)
    {
        return tryPublishEvents(translators, 0, translators.length);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslator[], int, int)
     */
    @Override
    public boolean tryPublishEvents(final EventTranslator<E>[] translators, final int batchStartsAt, final int batchSize)
    {
        checkBounds(translators, batchStartsAt, batchSize);
        try
        {
            final long finalSequence = sequencer.tryNext(batchSize);
            translateAndPublishBatch(translators, batchStartsAt, batchSize, finalSequence);
            return true;
        }
        catch (InsufficientCapacityException e)
        {
            return false;
        }
    }

    /**
     * @see com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslatorOneArg, Object[])
     * com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslatorOneArg, A[])
     */
    @Override
    public <A> void publishEvents(final EventTranslatorOneArg<E, A> translator, final A[] arg0)
    {
        publishEvents(translator, 0, arg0.length, arg0);
    }

    /**
     * @see com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslatorOneArg, int, int, Object[])
     * com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslatorOneArg, int, int, A[])
     */
    @Override
    public <A> void publishEvents(final EventTranslatorOneArg<E, A> translator, final int batchStartsAt, final int batchSize, final A[] arg0)
    {
        checkBounds(arg0, batchStartsAt, batchSize);
        final long finalSequence = sequencer.next(batchSize);
        translateAndPublishBatch(translator, arg0, batchStartsAt, batchSize, finalSequence);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslatorOneArg, Object[])
     * com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslatorOneArg, A[])
     */
    @Override
    public <A> boolean tryPublishEvents(final EventTranslatorOneArg<E, A> translator, final A[] arg0)
    {
        return tryPublishEvents(translator, 0, arg0.length, arg0);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslatorOneArg, int, int, Object[])
     * com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslatorOneArg, int, int, A[])
     */
    @Override
    public <A> boolean tryPublishEvents(
            final EventTranslatorOneArg<E, A> translator, final int batchStartsAt, final int batchSize, final A[] arg0)
    {
        checkBounds(arg0, batchStartsAt, batchSize);
        try
        {
            final long finalSequence = sequencer.tryNext(batchSize);
            translateAndPublishBatch(translator, arg0, batchStartsAt, batchSize, finalSequence);
            return true;
        }
        catch (InsufficientCapacityException e)
        {
            return false;
        }
    }

    /**
     * @see com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslatorTwoArg, Object[], Object[])
     * com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslatorTwoArg, A[], B[])
     */
    @Override
    public <A, B> void publishEvents(final EventTranslatorTwoArg<E, A, B> translator, final A[] arg0, final B[] arg1)
    {
        publishEvents(translator, 0, arg0.length, arg0, arg1);
    }

    /**
     * @see com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslatorTwoArg, int, int, Object[], Object[])
     * com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslatorTwoArg, int, int, A[], B[])
     */
    @Override
    public <A, B> void publishEvents(
        final EventTranslatorTwoArg<E, A, B> translator, final int batchStartsAt, final int batchSize, final A[] arg0, final B[] arg1)
    {
        checkBounds(arg0, arg1, batchStartsAt, batchSize);
        final long finalSequence = sequencer.next(batchSize);
        translateAndPublishBatch(translator, arg0, arg1, batchStartsAt, batchSize, finalSequence);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslatorTwoArg, Object[], Object[])
     * com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslatorTwoArg, A[], B[])
     */
    @Override
    public <A, B> boolean tryPublishEvents(final EventTranslatorTwoArg<E, A, B> translator, final A[] arg0, final B[] arg1)
    {
        return tryPublishEvents(translator, 0, arg0.length, arg0, arg1);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslatorTwoArg, int, int, Object[], Object[])
     * com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslatorTwoArg, int, int, A[], B[])
     */
    @Override
    public <A, B> boolean tryPublishEvents(
        final EventTranslatorTwoArg<E, A, B> translator, final int batchStartsAt, final int batchSize, final A[] arg0, final B[] arg1)
    {
        checkBounds(arg0, arg1, batchStartsAt, batchSize);
        try
        {
            final long finalSequence = sequencer.tryNext(batchSize);
            translateAndPublishBatch(translator, arg0, arg1, batchStartsAt, batchSize, finalSequence);
            return true;
        }
        catch (InsufficientCapacityException e)
        {
            return false;
        }
    }

    /**
     * @see com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslatorThreeArg, Object[], Object[], Object[])
     * com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslatorThreeArg, A[], B[], C[])
     */
    @Override
    public <A, B, C> void publishEvents(final EventTranslatorThreeArg<E, A, B, C> translator, final A[] arg0, final B[] arg1, final C[] arg2)
    {
        publishEvents(translator, 0, arg0.length, arg0, arg1, arg2);
    }

    /**
     * @see com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslatorThreeArg, int, int, Object[], Object[], Object[])
     * com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslatorThreeArg, int, int, A[], B[], C[])
     */
    @Override
    public <A, B, C> void publishEvents(
        final EventTranslatorThreeArg<E, A, B, C> translator, final int batchStartsAt, final int batchSize, final A[] arg0, final B[] arg1, final C[] arg2)
    {
        checkBounds(arg0, arg1, arg2, batchStartsAt, batchSize);
        final long finalSequence = sequencer.next(batchSize);
        translateAndPublishBatch(translator, arg0, arg1, arg2, batchStartsAt, batchSize, finalSequence);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslatorThreeArg, Object[], Object[], Object[])
     * com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslatorThreeArg, A[], B[], C[])
     */
    @Override
    public <A, B, C> boolean tryPublishEvents(
        final EventTranslatorThreeArg<E, A, B, C> translator, final A[] arg0, final B[] arg1, final C[] arg2)
    {
        return tryPublishEvents(translator, 0, arg0.length, arg0, arg1, arg2);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslatorThreeArg, int, int, Object[], Object[], Object[])
     * com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslatorThreeArg, int, int, A[], B[], C[])
     */
    @Override
    public <A, B, C> boolean tryPublishEvents(
        final EventTranslatorThreeArg<E, A, B, C> translator, final int batchStartsAt, final int batchSize, final A[] arg0, final B[] arg1, final C[] arg2)
    {
        checkBounds(arg0, arg1, arg2, batchStartsAt, batchSize);
        try
        {
            final long finalSequence = sequencer.tryNext(batchSize);
            translateAndPublishBatch(translator, arg0, arg1, arg2, batchStartsAt, batchSize, finalSequence);
            return true;
        }
        catch (InsufficientCapacityException e)
        {
            return false;
        }
    }

    /**
     * @see com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslatorVararg, java.lang.Object[][])
     */
    @Override
    public void publishEvents(final EventTranslatorVararg<E> translator, final Object[]... args)
    {
        publishEvents(translator, 0, args.length, args);
    }

    /**
     * @see com.lmax.disruptor.EventSink#publishEvents(com.lmax.disruptor.EventTranslatorVararg, int, int, java.lang.Object[][])
     */
    @Override
    public void publishEvents(final EventTranslatorVararg<E> translator, final int batchStartsAt, final int batchSize, final Object[]... args)
    {
        checkBounds(batchStartsAt, batchSize, args);
        final long finalSequence = sequencer.next(batchSize);
        translateAndPublishBatch(translator, batchStartsAt, batchSize, finalSequence, args);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslatorVararg, java.lang.Object[][])
     */
    @Override
    public boolean tryPublishEvents(final EventTranslatorVararg<E> translator, final Object[]... args)
    {
        return tryPublishEvents(translator, 0, args.length, args);
    }

    /**
     * @see com.lmax.disruptor.EventSink#tryPublishEvents(com.lmax.disruptor.EventTranslatorVararg, int, int, java.lang.Object[][])
     */
    @Override
    public boolean tryPublishEvents(
        final EventTranslatorVararg<E> translator, final int batchStartsAt, final int batchSize, final Object[]... args)
    {
        checkBounds(args, batchStartsAt, batchSize);
        try
        {
            final long finalSequence = sequencer.tryNext(batchSize);
            translateAndPublishBatch(translator, batchStartsAt, batchSize, finalSequence, args);
            return true;
        }
        catch (InsufficientCapacityException e)
        {
            return false;
        }
    }

    /**
     * Publish the specified sequence.  This action marks this particular
     * message as being available to be read.
     * 发布指定的序列。此操作将此特定消息标记为可阅读
     * @param sequence the sequence to publish.
     */
    @Override
    public void publish(final long sequence)
    {
        sequencer.publish(sequence);
    }

    /**
     * Publish the specified sequences.  This action marks these particular
     * messages as being available to be read.
     *
     * @param lo the lowest sequence number to be published
     * @param hi the highest sequence number to be published
     * @see Sequencer#next(int)
     */
    @Override
    public void publish(final long lo, final long hi)
    {
        sequencer.publish(lo, hi);
    }

    /**
     * Get the remaining capacity for this ringBuffer.
     *
     * @return The number of slots remaining.
     */
    @Override
    public long remainingCapacity()
    {
        return sequencer.remainingCapacity();
    }

    private void checkBounds(final EventTranslator<E>[] translators, final int batchStartsAt, final int batchSize)
    {
        checkBatchSizing(batchStartsAt, batchSize);
        batchOverRuns(translators, batchStartsAt, batchSize);
    }

    private void checkBatchSizing(final int batchStartsAt, final int batchSize)
    {
        if (batchStartsAt < 0 || batchSize < 0)
        {
            throw new IllegalArgumentException("Both batchStartsAt and batchSize must be positive but got: batchStartsAt " + batchStartsAt + " and batchSize " + batchSize);
        }
        else if (batchSize > bufferSize)
        {
            throw new IllegalArgumentException("The ring buffer cannot accommodate " + batchSize + " it only has space for " + bufferSize + " entities.");
        }
    }

    private <A> void checkBounds(final A[] arg0, final int batchStartsAt, final int batchSize)
    {
        checkBatchSizing(batchStartsAt, batchSize);
        batchOverRuns(arg0, batchStartsAt, batchSize);
    }

    private <A, B> void checkBounds(final A[] arg0, final B[] arg1, final int batchStartsAt, final int batchSize)
    {
        checkBatchSizing(batchStartsAt, batchSize);
        batchOverRuns(arg0, batchStartsAt, batchSize);
        batchOverRuns(arg1, batchStartsAt, batchSize);
    }

    private <A, B, C> void checkBounds(
        final A[] arg0, final B[] arg1, final C[] arg2, final int batchStartsAt, final int batchSize)
    {
        checkBatchSizing(batchStartsAt, batchSize);
        batchOverRuns(arg0, batchStartsAt, batchSize);
        batchOverRuns(arg1, batchStartsAt, batchSize);
        batchOverRuns(arg2, batchStartsAt, batchSize);
    }

    private void checkBounds(final int batchStartsAt, final int batchSize, final Object[][] args)
    {
        checkBatchSizing(batchStartsAt, batchSize);
        batchOverRuns(args, batchStartsAt, batchSize);
    }

    private <A> void batchOverRuns(final A[] arg0, final int batchStartsAt, final int batchSize)
    {
        if (batchStartsAt + batchSize > arg0.length)
        {
            throw new IllegalArgumentException(
                "A batchSize of: " + batchSize +
                    " with batchStatsAt of: " + batchStartsAt +
                    " will overrun the available number of arguments: " + (arg0.length - batchStartsAt));
        }
    }

    /**
     * 转换并发布
     * @param translator
     * @param sequence
     */
    private void translateAndPublish(final EventTranslator<E> translator, final long sequence)
    {
        try
        {
            translator.translateTo(get(sequence), sequence);
        }
        finally
        {
            sequencer.publish(sequence);
        }
    }

    private <A> void translateAndPublish(final EventTranslatorOneArg<E, A> translator, final long sequence, final A arg0)
    {
        try
        {
            translator.translateTo(get(sequence), sequence, arg0);
        }
        finally
        {
            sequencer.publish(sequence);
        }
    }

    private <A, B> void translateAndPublish(final EventTranslatorTwoArg<E, A, B> translator, final long sequence, final A arg0, final B arg1)
    {
        try
        {
            translator.translateTo(get(sequence), sequence, arg0, arg1);
        }
        finally
        {
            sequencer.publish(sequence);
        }
    }

    private <A, B, C> void translateAndPublish(
        final EventTranslatorThreeArg<E, A, B, C> translator, final long sequence,
        final A arg0, final B arg1, final C arg2)
    {
        try
        {
            translator.translateTo(get(sequence), sequence, arg0, arg1, arg2);
        }
        finally
        {
            sequencer.publish(sequence);
        }
    }

    private void translateAndPublish(final EventTranslatorVararg<E> translator, final long sequence, final Object... args)
    {
        try
        {
            translator.translateTo(get(sequence), sequence, args);
        }
        finally
        {
            sequencer.publish(sequence);
        }
    }

    private void translateAndPublishBatch(
        final EventTranslator<E>[] translators, final int batchStartsAt,
        final int batchSize, final long finalSequence)
    {
        final long initialSequence = finalSequence - (batchSize - 1);
        try
        {
            long sequence = initialSequence;
            final int batchEndsAt = batchStartsAt + batchSize;
            for (int i = batchStartsAt; i < batchEndsAt; i++)
            {
                final EventTranslator<E> translator = translators[i];
                translator.translateTo(get(sequence), sequence++);
            }
        }
        finally
        {
            sequencer.publish(initialSequence, finalSequence);
        }
    }

    private <A> void translateAndPublishBatch(
        final EventTranslatorOneArg<E, A> translator, final A[] arg0,
        final int batchStartsAt, final int batchSize, final long finalSequence)
    {
        final long initialSequence = finalSequence - (batchSize - 1);
        try
        {
            long sequence = initialSequence;
            final int batchEndsAt = batchStartsAt + batchSize;
            for (int i = batchStartsAt; i < batchEndsAt; i++)
            {
                translator.translateTo(get(sequence), sequence++, arg0[i]);
            }
        }
        finally
        {
            sequencer.publish(initialSequence, finalSequence);
        }
    }

    private <A, B> void translateAndPublishBatch(
        final EventTranslatorTwoArg<E, A, B> translator, final A[] arg0,
        final B[] arg1, final int batchStartsAt, final int batchSize,
        final long finalSequence)
    {
        final long initialSequence = finalSequence - (batchSize - 1);
        try
        {
            long sequence = initialSequence;
            final int batchEndsAt = batchStartsAt + batchSize;
            for (int i = batchStartsAt; i < batchEndsAt; i++)
            {
                translator.translateTo(get(sequence), sequence++, arg0[i], arg1[i]);
            }
        }
        finally
        {
            sequencer.publish(initialSequence, finalSequence);
        }
    }

    private <A, B, C> void translateAndPublishBatch(
        final EventTranslatorThreeArg<E, A, B, C> translator,
        final A[] arg0, final B[] arg1, final C[] arg2, final int batchStartsAt,
        final int batchSize, final long finalSequence)
    {
        final long initialSequence = finalSequence - (batchSize - 1);
        try
        {
            long sequence = initialSequence;
            final int batchEndsAt = batchStartsAt + batchSize;
            for (int i = batchStartsAt; i < batchEndsAt; i++)
            {
                translator.translateTo(get(sequence), sequence++, arg0[i], arg1[i], arg2[i]);
            }
        }
        finally
        {
            sequencer.publish(initialSequence, finalSequence);
        }
    }

    private void translateAndPublishBatch(
        final EventTranslatorVararg<E> translator, final int batchStartsAt,
        final int batchSize, final long finalSequence, final Object[][] args)
    {
        final long initialSequence = finalSequence - (batchSize - 1);
        try
        {
            long sequence = initialSequence;
            final int batchEndsAt = batchStartsAt + batchSize;
            for (int i = batchStartsAt; i < batchEndsAt; i++)
            {
                translator.translateTo(get(sequence), sequence++, args[i]);
            }
        }
        finally
        {
            sequencer.publish(initialSequence, finalSequence);
        }
    }

    @Override
    public String toString()
    {
        return "RingBuffer{" +
            "bufferSize=" + bufferSize +
            ", sequencer=" + sequencer +
            "}";
    }
}
