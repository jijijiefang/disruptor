package com.lmax.disruptor;


import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * 缓存行左填充
 */
class LhsPadding
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
 * 缓存行保存实际的值
 */
class Value extends LhsPadding
{
    protected long value;
}

/**
 * 缓存行右填充
 */
class RhsPadding extends Value
{
    protected byte
        p90, p91, p92, p93, p94, p95, p96, p97,
        p100, p101, p102, p103, p104, p105, p106, p107,
        p110, p111, p112, p113, p114, p115, p116, p117,
        p120, p121, p122, p123, p124, p125, p126, p127,
        p130, p131, p132, p133, p134, p135, p136, p137,
        p140, p141, p142, p143, p144, p145, p146, p147,
        p150, p151, p152, p153, p154, p155, p156, p157;
}

/**
 * Concurrent sequence class used for tracking the progress of
 * the ring buffer and event processors.  Support a number
 * of concurrent operations including CAS and order writes.
 * 并行序列类用于循环缓冲区和事件处理器跟踪进度。支持许多并发操作，包括CAS和顺序写。
 * <p>Also attempts to be more efficient with regards to false
 * sharing by adding padding around the volatile field.
 * 更有效的处理伪共享通过在volatile字段周围添加填充
 */
public class Sequence extends RhsPadding
{
    static final long INITIAL_VALUE = -1L;
    private static final VarHandle VALUE_FIELD;

    static
    {
        try
        {
            VALUE_FIELD = MethodHandles.lookup().in(Sequence.class)
                    .findVarHandle(Sequence.class, "value", long.class);
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a sequence initialised to -1.
     * 使用-1初始化创建序列
     */
    public Sequence()
    {
        this(INITIAL_VALUE);
    }

    /**
     * Create a sequence with a specified initial value.
     * 使用特定的初始值创建序列
     *
     * @param initialValue The initial value for this sequence.
     */
    public Sequence(final long initialValue)
    {
        //确保此屏障前的读取和写入不会与屏障后的写入重排序 StoreStore+LoadStore
        VarHandle.releaseFence();
        this.value = initialValue;
    }

    /**
     * Perform a volatile read of this sequence's value.
     * 使用volatile方式读取序列值
     *
     * @return The current value of the sequence.
     */
    public long get()
    {
        long value = this.value;
        //确保屏障前的读取不会与屏障后的读取和写入一起重新排序，LoadLoad+LoadStore
        VarHandle.acquireFence();
        return value;
    }

    /**
     * Perform an ordered write of this sequence.  The intent is
     * a Store/Store barrier between this write and any previous
     * store.
     * 执行此序列的有序写入。其目的是在该写入和任何以前的存储之间设置存储/存储屏障。使用volatile的方式设置序列值
     *
     * @param value The new value for the sequence.
     */
    public void set(final long value)
    {
        //确保此屏障前的读取和写入不会与屏障后的写入重排序 StoreStore+LoadStore
        VarHandle.releaseFence();
        this.value = value;
    }

    /**
     * Performs a volatile write of this sequence.  The intent is
     * a Store/Store barrier between this write and any previous
     * write and a Store/Load barrier between this write and any
     * subsequent volatile read.
     * 执行此序列的volatile写入。其目的是在本次写入和任何之前的写入之间设置存储/存储屏障，并在本次写入和任何后续的volatile读取之间设置存储/加载屏障。
     * @param value The new value for the sequence.
     */
    public void setVolatile(final long value)
    {
        //确保此屏障前的读取和写入不会与屏障后的写入重排序 StoreStore+LoadStore
        VarHandle.releaseFence();
        this.value = value;
        //确保屏障前的读取和写入不会与屏障后的读取和写入一起重新排序，表示LoadLoad+LoadStore和StoreStore+LoadStore的效果，此外，还表示StoreLoad屏障的效果
        VarHandle.fullFence();
    }

    /**
     * Perform a compare and set operation on the sequence.
     * 使用CAS方式设置序列值
     * @param expectedValue The expected current value.
     * @param newValue      The value to update to.
     * @return true if the operation succeeds, false otherwise.
     */
    public boolean compareAndSet(final long expectedValue, final long newValue)
    {
        return VALUE_FIELD.compareAndSet(this, expectedValue, newValue);
    }

    /**
     * Atomically increment the sequence by one.
     *
     * @return The value after the increment
     */
    public long incrementAndGet()
    {
        return addAndGet(1);
    }

    /**
     * Atomically add the supplied value.
     * 原子地添加提供的值
     * @param increment The value to add to the sequence.
     * @return The value after the increment.
     */
    public long addAndGet(final long increment)
    {
        return (long) VALUE_FIELD.getAndAdd(this, increment) + increment;
    }

    /**
     * Perform an atomic getAndAdd operation on the sequence.
     * 对序列执行原子getAndAdd操作
     * @param increment The value to add to the sequence.
     * @return the value before increment
     */
    public long getAndAdd(final long increment)
    {
        return (long) VALUE_FIELD.getAndAdd(this, increment);
    }

    @Override
    public String toString()
    {
        return Long.toString(get());
    }
}