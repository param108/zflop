////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author Clement Wong
 */
public class Benchmark
{
	private long start = 0;
	private long begin = 0;

	public void start()
	{
		start = System.currentTimeMillis();
		begin = start;
	}

	public void benchmark(String message)
	{
		long currentTime = System.currentTimeMillis();
		if (start != 0)
		{
			ThreadLocalToolkit.log(new BenchmarkText(message, currentTime - start));
		}
		start = currentTime;

		if (begin == 0)
		{
			begin = currentTime;
		}
	}

	public void totalTime()
	{
		ThreadLocalToolkit.log(new TotalTime(System.currentTimeMillis() - begin));
	}

    private HashMap times;

    public final long stopTime(String id)
    {
        long currentTime = System.currentTimeMillis();
        Long time = ((Long)times.get(id));
        if (time == null)
            throw new IllegalStateException("Call startTime before calling stopTime");
        long previousTime = time.longValue();
        long duration = currentTime - previousTime;
	    ThreadLocalToolkit.log(new BenchmarkID(id, duration));
        startTime(id);
        return duration;
    }

    public final void startTime(String id)
    {
        if (times == null)
            times = new HashMap();
        times.put(id, new Long(System.currentTimeMillis()));
    }

	/**
	 * @return peak memory usage in Mb
	 */
    public final long peakMemoryUsage()
    {
        return peakMemoryUsage(true);
    }

    public final long peakMemoryUsage(boolean display)
    {
	    MemoryUsage mem = getMemoryUsageInBytes();
	    long mbHeapUsed = (mem.heap / 1048576);
		long mbNonHeapUsed = (mem.nonHeap / 1048576);

	    if (display && mem.heap != 0 && mem.nonHeap != 0)
	    {
		    ThreadLocalToolkit.log(new MemoryUsage(mbHeapUsed, mbNonHeapUsed));
	    }

	    return mbHeapUsed + mbNonHeapUsed;
    }

	public final long peakMemoryUsageInBytes()
	{
	    return peakMemoryUsage(true);
	}

	public final long peakMemoryUsageInBytes(boolean display)
	{
		MemoryUsage mem = getMemoryUsageInBytes();

		if (display && mem.heap != 0 && mem.nonHeap != 0)
		{
			ThreadLocalToolkit.log(mem);
		}

		return mem.total;
	}

	/**
	 * @return peak memory usage in bytes
	 */
	public MemoryUsage getMemoryUsageInBytes()
	{
		long heapUsed = 0, nonHeapUsed = 0;

	    try
	    {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
	        Class mfCls = Class.forName("java.lang.management.ManagementFactory", true, contextClassLoader);
	        Class mpCls = Class.forName("java.lang.management.MemoryPoolMXBean", true, contextClassLoader);
	        Class memCls = Class.forName("java.lang.management.MemoryUsage", true, contextClassLoader);
		    Class typeCls = Class.forName("java.lang.management.MemoryType", true, contextClassLoader);

	        Class[] emptyCls = new Class[] {};
	        Object[] emptyObj = new Object[] {};
	        Method getMemPoolMeth = mfCls.getMethod("getMemoryPoolMXBeans", emptyCls);
	        Method getPeakUsageMeth = mpCls.getMethod("getPeakUsage", emptyCls);
		    Method getTypeMeth = mpCls.getMethod("getType", emptyCls);
		    Field heapField = typeCls.getField("HEAP");
	        Method getUsedMeth = memCls.getMethod("getUsed", emptyCls);

	        List list = (List)getMemPoolMeth.invoke(null, emptyObj);
	        for (Iterator iterator = list.iterator(); iterator.hasNext();)
	        {
	            Object memPoolObj = (Object)iterator.next();
	            Object memUsageObj = getPeakUsageMeth.invoke(memPoolObj, emptyObj);
		        Object memTypeObj = getTypeMeth.invoke(memPoolObj, emptyObj);
		        Long used = (Long)getUsedMeth.invoke(memUsageObj, emptyObj);
		        if (heapField.get(typeCls) == memTypeObj)
		        {
		            heapUsed += used.longValue();
		        }
		        else
		        {
			        nonHeapUsed += used.longValue();
		        }
	        }

		    resetPeakMemoryUsage();
	    }
	    catch(Exception e)
	    {
	        // ignore, assume not using jdk 1.5
	    }

		return new MemoryUsage(heapUsed, nonHeapUsed);
	}

	private void resetPeakMemoryUsage()
	{
		try
		{
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		    Class mfCls = Class.forName("java.lang.management.ManagementFactory", true, contextClassLoader);
		    Class mpCls = Class.forName("java.lang.management.MemoryPoolMXBean", true, contextClassLoader);

		    Class[] emptyCls = new Class[] {};
		    Object[] emptyObj = new Object[] {};
		    Method getMemPoolMeth = mfCls.getMethod("getMemoryPoolMXBeans", emptyCls);
			Method resetPeakUsageMeth = mpCls.getMethod("resetPeakUsage", emptyCls);

		    List list = (List)getMemPoolMeth.invoke(null, emptyObj);
		    for (Iterator iterator = list.iterator(); iterator.hasNext();)
		    {
		        Object memPoolObj = (Object)iterator.next();
			    resetPeakUsageMeth.invoke(memPoolObj, emptyObj);
		    }
		}
		catch(Exception e)
		{
		    // ignore, assume not using jdk 1.5
		}
	}

	public void captureMemorySnapshot()
	{		
	}
	
	// error messages

	public static class BenchmarkText extends CompilerMessage.CompilerInfo
	{
		public BenchmarkText(String message, long time)
		{
			super();
			this.message = message;
			this.time = time;
		}

		public final String message;
		public final long time;
	}

	public static class BenchmarkID extends CompilerMessage.CompilerInfo
	{
		public BenchmarkID(String id, long duration)
		{
			super();
			this.id = id;
			this.duration = duration;
		}

		public final String id;
		public final long duration;
	}

	public static class TotalTime extends CompilerMessage.CompilerInfo
	{
		public TotalTime(long time)
		{
			super();
			this.time = time;
		}

		public final long time;
	}

	public static class MemoryUsage extends CompilerMessage.CompilerInfo
	{
		public MemoryUsage(long heap, long nonHeap)
		{
			super();
			this.heap = heap;
			this.nonHeap = nonHeap;
			this.total = heap + nonHeap;
		}

		public long heap, nonHeap, total;

		public void add(MemoryUsage mem)
		{
			this.heap += mem.heap;
			this.nonHeap += mem.nonHeap;
			this.total += mem.total;
		}

		public void subtract(MemoryUsage mem)
		{
			this.heap -= mem.heap;
			this.nonHeap -= mem.nonHeap;
			this.total -= mem.total;
		}
	}
}
