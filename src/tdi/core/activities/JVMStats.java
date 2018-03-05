/*
 * Copyright (c) 2010-2011 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.core.activities;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;

public class JVMStats {
	
	private final static HashMap<String,Long> gc = new HashMap<String, Long>();
	private static volatile long time0 = 0;
	private static volatile double usage0 = 0;
	
	public final static double getPercentMemUsage() {
		MemoryMXBean mb = ManagementFactory.getMemoryMXBean();
		double r = (mb.getHeapMemoryUsage().getUsed() + mb.getNonHeapMemoryUsage().getUsed())*100;
		double res = Math.rint(100*r/(mb.getHeapMemoryUsage().getMax() + mb.getNonHeapMemoryUsage().getMax()))/100.0;
		return Double.isInfinite(res) || Double.isNaN(res) ? 0.0 : res;
	}
	
	public final static double getPercentGcUsage() {
		Long v0 = gc.get("jvm");
		if (v0==null)
			v0 = 0L;
		long v1 = System.currentTimeMillis();
		double all = 0;
		
		for (GarbageCollectorMXBean gcmb: ManagementFactory.getGarbageCollectorMXBeans()) {
			Long val0 = gc.get(gcmb.getName());
			if (val0==null)
				val0 = 0L;
			long val1 = gcmb.getCollectionTime();
			all += (val1-val0);
			gc.put(gcmb.getName(), val1);			
		}
		gc.put("jvm", v1);
		double res = Math.rint(all*10000/(v1-v0))/100.0;
		return Double.isInfinite(res) || Double.isNaN(res) ? 0.0 : res;
	}
	
	public final static double getJvmCpuUsage() {
		OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean( );
		if (!(bean instanceof com.sun.management.OperatingSystemMXBean))
			return 0L;
		long time1 = System.currentTimeMillis();
	    long usage1 = ((com.sun.management.OperatingSystemMXBean)bean).getProcessCpuTime()/1000000;
	    double res = Math.rint(10000*(usage1-usage0)/(time1-time0))/100.0;
	    time0 = time1;
	    usage0 = usage1;
	    return Double.isInfinite(res) || Double.isNaN(res) ? 0.0 : res;	    
	}
	
	public final static long getJvmUptime() {
		return ManagementFactory.getRuntimeMXBean().getUptime();
	}	
}
