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
 * Coordination barrier for tracking the cursor for publishers and sequence of
 * dependent {@link EventProcessor}s for processing a data structure
 * 用于跟踪发布者光标的协调屏障，以及用于处理数据结构的从属事件处理器序列
 */
public interface SequenceBarrier
{
    /**
     * Wait for the given sequence to be available for consumption.
     * 等待给定序列可供使用，然后消费这个序列
     * @param sequence to wait for
     * @return the sequence up to which is available
     * @throws AlertException       if a status change has occurred for the Disruptor
     * @throws InterruptedException if the thread needs awaking on a condition variable.
     * @throws TimeoutException     if a timeout occurs while waiting for the supplied sequence.
     */
    long waitFor(long sequence) throws AlertException, InterruptedException, TimeoutException;

    /**
     * Get the current cursor value that can be read.
     * 获取可读取的当前序列值
     * @return value of the cursor for entries that have been published.
     */
    long getCursor();

    /**
     * The current alert status for the barrier.
     * 栅栏的当前通知状态
     * @return true if in alert otherwise false.
     */
    boolean isAlerted();

    /**
     * Alert the {@link EventProcessor}s of a status change and stay in this status until cleared.
     * 提醒EventProcessors状态发生变化，并保持此状态，直到清除为止
     */
    void alert();

    /**
     * Clear the current alert status.
     * 清除当前通知状态
     */
    void clearAlert();

    /**
     * Check if an alert has been raised and throw an {@link AlertException} if it has.
     * 检查是否已引发通知，如果已引发，则抛出AlertException
     * @throws AlertException if alert has been raised.
     */
    void checkAlert() throws AlertException;
}
