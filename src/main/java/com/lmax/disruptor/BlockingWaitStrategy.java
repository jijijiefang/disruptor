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

/**
 * Blocking strategy that uses a lock and condition variable for {@link EventProcessor}s waiting on a barrier.
 * 阻塞策略，对等待屏障的事件处理器使用锁和条件变量。
 * <p>This strategy can be used when throughput and low-latency are not as important as CPU resource.
 * 当吞吐量和低延迟不如CPU资源重要时，可以使用此策略。
 */
public final class BlockingWaitStrategy implements WaitStrategy
{
    private final Object mutex = new Object();

    /**
     * 等待条件满足
     * @param sequence 等待的序列目标值
     * @param cursorSequence 当前游标序列值
     * @param dependentSequence 在哪个序列上等待
     * @param barrier 消费者等待序列栅栏
     * @return
     * @throws AlertException
     * @throws InterruptedException
     */
    @Override
    public long waitFor(final long sequence, final Sequence cursorSequence, final Sequence dependentSequence, final SequenceBarrier barrier)
        throws AlertException, InterruptedException
    {
        long availableSequence;
        //如果游标值小于目标值
        if (cursorSequence.get() < sequence)
        {
            //加锁
            synchronized (mutex)
            {
                //循环判断
                while (cursorSequence.get() < sequence)
                {
                    barrier.checkAlert();
                    //阻塞等待
                    mutex.wait();
                }
            }
        }
        //如果游标值大于等于目标值 且 依赖序列小于目标值
        while ((availableSequence = dependentSequence.get()) < sequence)
        {
            barrier.checkAlert();
            //自旋等待
            Thread.onSpinWait();
        }

        return availableSequence;
    }

    /**
     * 唤醒所有阻塞的线程
     */
    @Override
    public void signalAllWhenBlocking()
    {
        synchronized (mutex)
        {
            mutex.notifyAll();
        }
    }

    @Override
    public String toString()
    {
        return "BlockingWaitStrategy{" +
            "mutex=" + mutex +
            '}';
    }
}
