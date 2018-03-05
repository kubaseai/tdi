/*
 * Copyright (c) 2010-2011 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.test;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.locks.LockSupport;

public class TimerResolution {
	
	public final static void main(String[] args) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		TreeMap<Long,Long> map = new TreeMap<Long,Long>();
		for (int i=0; i < 15000; i++) {
			long t0 = System.nanoTime();
			try {
				LockSupport.parkNanos(1000000);
			}
			catch (Exception e) {}
			long dt = (System.nanoTime()-t0)/1000;
			map.put(dt, dt);			
		}
		int mid = map.size()/2;
		Iterator<Entry<Long, Long>> it = map.entrySet().iterator();
		for (int i = 0; i < mid; i++)
			it.next();
		System.out.println("Timer resolution: "+it.next().getKey()+" ["+map.size()+"]");
	}
}
