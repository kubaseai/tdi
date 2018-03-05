package com.tibco.pe.core;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public class RecoverableJobData {
	
	public String processDef;
	public String trackingId;
	public String customId;
	public String restartActivity;
	public long jobId = 0;
	String dupKey;
	public boolean faulted;
	
	RecoverableJobData(long id, String process, String trId, String cId, String activity,
				String dupKey, boolean faulted) {
		jobId = id;
		processDef = process;
		trackingId = trId;
		customId = cId;
		restartActivity = activity;	
		this.dupKey = dupKey;
		this.faulted = faulted;
	}
}
