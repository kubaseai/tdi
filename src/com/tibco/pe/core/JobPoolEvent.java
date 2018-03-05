package com.tibco.pe.core;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public class JobPoolEvent {

	public long jid = 0;

	public void addJobListener(JobListener tjl) {}
	
	public int getState() { return 0; }

	public void removeJobListener(JobListener tjl) {}
}
