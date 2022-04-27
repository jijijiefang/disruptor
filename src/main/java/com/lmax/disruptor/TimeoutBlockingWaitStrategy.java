package com.lmax.disruptor;

import java.util.concurrent.TimeUnit;

import static com.lmax.disruptor.util.Util.awaitNanos;

/**
 * Blocking strategy that uses a lock and condition variable for {@link EventProcessor}s waiting on a barrier.
 * However it will periodically wake up if it has been idle for specified period by throwing a
 * {@link TimeoutException}.  To make use of this, the event handler class should override
 * {@link EventHandler#onTimeout(long)}, which the {@link BatchEventProcessor} will call if the timeout occurs.
 * 阻塞策略，对等待屏障的事件处理器使用锁和条件变量。但是，如果它在指定的时间段内处于空闲状态，它将通过抛出TimeoutException定期唤醒。
 * 为了利用这一点，事件处理程序类应该重写EventHandler.onTimeout（long），如果发生超时，BatchEventProcessor将调用它。
 * <p>This strategy can be used when throughput and low-latency are not as important as CPU resource.
 * 当吞吐量和低延迟不如CPU资源重要时，可以使用此策略
 */
public class TimeoutBlockingWaitStrategy implements WaitStrategy
{
    private final Object mutex = new Object();
    private final long timeoutInNanos;

    /**
     * @param timeout how long to wait before waking up
     * @param units the unit in which timeout is specified
     */
    public TimeoutBlockingWaitStrategy(final long timeout, final TimeUnit units)
    {
        timeoutInNanos = units.toNanos(timeout);
    }

    @Override
    public long waitFor(
        final long sequence,
        final Sequence cursorSequence,
        final Sequence dependentSequence,
        final SequenceBarrier barrier)
        throws AlertException, InterruptedException, TimeoutException
    {
        long timeoutNanos = timeoutInNanos;

        long availableSequence;
        if (cursorSequence.get() < sequence)
        {
            synchronized (mutex)
            {
                while (cursorSequence.get() < sequence)
                {
                    barrier.checkAlert();
                    timeoutNanos = awaitNanos(mutex, timeoutNanos);
                    if (timeoutNanos <= 0)
                    {
                        throw TimeoutException.INSTANCE;
                    }
                }
            }
        }

        while ((availableSequence = dependentSequence.get()) < sequence)
        {
            barrier.checkAlert();
        }

        return availableSequence;
    }

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
        return "TimeoutBlockingWaitStrategy{" +
            "mutex=" + mutex +
            ", timeoutInNanos=" + timeoutInNanos +
            '}';
    }
}
