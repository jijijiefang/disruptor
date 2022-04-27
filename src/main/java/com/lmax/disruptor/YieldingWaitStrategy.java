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
 * Yielding strategy that uses a Thread.yield() for {@link com.lmax.disruptor.EventProcessor}s waiting on a barrier
 * after an initially spinning.
 * 使用线程的让步策略。初始旋转后等待屏障的事件处理器的yield()
 * <p>This strategy will use 100% CPU, but will more readily give up the CPU than a busy spin strategy if other threads
 * require CPU resource.
 * 此策略将使用100%的CPU，但如果其他线程需要CPU资源，则会比繁忙的旋转策略更容易放弃CPU。
 */
public final class YieldingWaitStrategy implements WaitStrategy
{
    private static final int SPIN_TRIES = 100;

    @Override
    public long waitFor(
        final long sequence, final Sequence cursor, final Sequence dependentSequence, final SequenceBarrier barrier)
        throws AlertException, InterruptedException
    {
        long availableSequence;
        int counter = SPIN_TRIES;
        //依赖序列小于目标序列自旋，自旋100次后Thread.yield()
        while ((availableSequence = dependentSequence.get()) < sequence)
        {
            counter = applyWaitMethod(barrier, counter);
        }

        return availableSequence;
    }

    @Override
    public void signalAllWhenBlocking()
    {
    }

    private int applyWaitMethod(final SequenceBarrier barrier, final int counter)
        throws AlertException
    {
        barrier.checkAlert();

        if (0 == counter)
        {
            Thread.yield();
        }
        else
        {
            return counter - 1;
        }

        return counter;
    }
}
