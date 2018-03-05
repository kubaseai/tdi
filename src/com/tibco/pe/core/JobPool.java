package com.tibco.pe.core;

import java.util.Enumeration;

import com.tibco.pe.dm.JobDataManager;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public class JobPool {
	
	public JobDataManager hibernateDataManager;
		
	public static String getFTName() {
		return "";
	}
	
	public Workflow getWorkflow(String name) {
	    return null;
	}
	 
	public WorkflowLoader getWorkflowLoader() {
		return null;
	}

	public int getThreadCount() {
		return 0;
	}

	public Job findJob(long jid) {
		return null;
	}
	
	public String killJob(String jid) throws Exception {
		return "not executed -- killJob is stub";
	}
	
	public void addJobPoolListener(JobPoolListener listener) {}
	
	public void removeJobPoolListener(JobPoolListener listener) {}

	public void setWorkflowLoader(WorkflowLoader wl) {}
	
	public void hibernateJob(Job job, String activity, Object restartCtx,
		JDBCConnectionEntry jdbcconnectionentry, String dupKey) {}
	
	public RecoverableJobData[] getHibernatedJobs() {
		return new RecoverableJobData[0];
	}
	
	public void restartHibernatedJob(long jid) throws Exception {}

	public void addJob(Job job, String string, boolean dispatch) {}

	public Enumeration<Object> getProcessStarters() {
		return null;
	}
	
	public long[] getJobIds() { return new long[0]; }
}
