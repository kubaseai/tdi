/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.core;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import tdi.core.activities.MarkerDefinition;
import tdi.transport.EventType;
import tdi.transport.SQLCache;
import tdi.transport.Transport;

public class WorkerThread extends Thread {
	
	private ConcurrentHashMap<Long, JobEventStats> jobListeners = null;
	private LinkedBlockingQueue<JobEventStats> channel = new LinkedBlockingQueue<JobEventStats>(1000);
	
	@ConfigParam(name="STALLED_JOB_TIME", desc="Treat job as stalled when it's not finished after given value of milliseconds", value="300000|integer")
	private static ValueHolder<Long> STALLED_JOB_TIME = PEProc.getVolatileLongProperty("STALLED_JOB_TIME", "300000");
	
	@ConfigParam(name="PING_TIME", desc="Emit ping event every given value of milliseconds", value="60000|integer")
	private static ValueHolder<Long> PING_TIME = PEProc.getVolatileLongProperty("PING_TIME", "60000");
	
	@ConfigParam(name="WORK_INTERVAL", desc="Process internal data every given value of milliseconds", value="250|integer")
	private static ValueHolder<Long> WORK_INTERVAL = PEProc.getVolatileLongProperty("WORK_INTERVAL", "250");
	
	@ConfigParam(name="MAX_EVENTS", desc="Process up to given value of events at once", value="1000|integer")
	private static ValueHolder<Long> MAX_EVENTS = PEProc.getVolatileLongProperty("MAX_EVENTS", "1000");
	
	private static AtomicBoolean includeMarkersDefs = new AtomicBoolean();
	private static AtomicBoolean shutdownFlag = new AtomicBoolean(false);
	@SuppressWarnings("unused")
	private static AtomicBoolean processFilesUploaded = new AtomicBoolean();
	
	public WorkerThread(ConcurrentHashMap<Long, JobEventStats> jobListeners) {
		this.jobListeners = jobListeners;
		this.setName("TDI WorkerThread #"+hashCode());
		try {
			setPriority( NORM_PRIORITY + 1 );
		}
		catch (Exception e) {}
		Runtime.getRuntime().addShutdownHook((Thread)JVMHelper.addShutdownHook(new Thread() {
			public void run() {
				shutdownFlag.set(true);
			}
		}));
	}
	
	public void addEvent(JobEventStats tje) {
		if (MAX_EVENTS.get() < 0)
			return;
		if (!channel.offer(tje))
			Logger.getInstance().debug("[ERROR] Event overflow for "+tje);	
	}

	public void onJobRemoved(JobEventStats tjl) {
		if (MAX_EVENTS.get() < 0)
			return;
		if (!channel.offer(tjl))
			Logger.getInstance().debug("[ERROR] Stats overflow for "+tjl.getProcessName());		
	}
	
	public void run() {
		long lastPing = 0;
		long lastStallCheck = 0;
		long lastResend = 0;
		while (true) {
			try {
				SQLCache.purgeOld();
				LinkedList<JobEventStats> list = new LinkedList<JobEventStats>();
			    JobEventStats jl = channel.poll(WORK_INTERVAL.get(), TimeUnit.MILLISECONDS);
			    while (jl!=null && list.size() < MAX_EVENTS.get()) {
			    	list.add(jl);
			    	jl = channel.poll(1, TimeUnit.SECONDS);
			    	if (jl!=null)
			    		list.add(jl);			    	
			    }	
			    
			    if (!list.isEmpty()) {
			    	if (!includeMarkersDefs.getAndSet(true))
			    		list.get(0).setIncludeMarkersDefs(true);
			    	if (PEProc.getRuntimeProperty("DEBUG", "0").equals("1"))
					   	Logger.getInstance().debug("Events: "+list);
			    	Transport.deliverEvents(EventType.AUTO, list);
			    	for (JobEventStats ev : list)
			    		ev.clear();
			    	list.clear();
			    }
			    
			    long now = System.currentTimeMillis();
			   			    
			    if (lastStallCheck > 0 && now - lastStallCheck >= STALLED_JOB_TIME.get()) {
				   	for (JobEventStats tjl : jobListeners.values()) {
				   		if (tjl.getRunningDuration() > STALLED_JOB_TIME.get()) {
				   			list.add(tjl.cloneLite().setTypeAndReturn(EventType.STALL));	
				   		}
				   	}				   	
				   	lastStallCheck = now;
			    }
			    if (now - lastPing >= PING_TIME.get()) {
			    	/* dummy event with real repoName */
			    	list.add(new JobEventStats(0).setTypeAndReturn(EventType.PING));
			    	list.getLast().getMetrics().put("scheduler", EngineHelperApi.getSchedulerSummary());
			    	lastPing = now;
			    	String oldMkDefs = MarkerDefinition.getRawString();
			    	Transport.loadAutoCfg();
			    	String newMkDefs = MarkerDefinition.getRawString();
			    	if (!oldMkDefs.equals(newMkDefs))
			    		includeMarkersDefs.set(true);
			    	PEProc.dispatchPopEvents(Transport.fetchEvents());
			    }
			    
			    if (!list.isEmpty())
			   		Transport.deliverEvents(EventType.AUTO, list);
			    
			    if (now - lastResend >= PING_TIME.get()) {
			    	Transport.resend(true);
			    	lastResend = now;
			    }
//			    if (!processFilesUploaded.get()) {
//			    	if (Transport.uploadProcesses(ProcessExtractor.retrieveProcessFiles()))
//			    		processFilesUploaded.set(true);			    	
//			    }
			}
			catch (InterruptedException ie) {}
			catch (OutOfMemoryError oom) {
				JVMHelper.oomExit(oom);
			}			
			finally {
				SQLCache.purgeOld();
			}
			if (shutdownFlag.get() && channel.size()==0)
				break;
		}		
	}
}
