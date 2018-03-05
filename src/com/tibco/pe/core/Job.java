package com.tibco.pe.core;

import com.tibco.pe.plugin.ActivityController;
import com.tibco.xml.datamodel.XiNode;
import com.tibco.xml.xdata.xpath.VariableList;

import tdi.core.CompileTimeStub;

@CompileTimeStub
final class Job {
	
	Long id = new Long(0);
	
	public Job(String id, Workflow workflow) {}
	
	protected VariableList getAttributes() {
		return null;
	}
	
	protected boolean ishibernated;
	
	public VariableList getCurrentAttributes() {
		return null;
	}

	public Workflow getActualWorkflow() {
		return null;
	}
	
	public boolean isActive() { return true; }
	public boolean isPaged() { return false; }
	public boolean isDead() { return false; }
	
	public void setReply(Object reply) {}
	public void setReply(int idx, Object obj) {}
	
	public ActivityController getActivityController() {
		return null;
	}
	
	public void moveOn(int trackId) {}
	public final int getTrackId() { return 0; }

	public void resume() {}
	
	public int callProcess(String workflow, XiNode inputData, boolean validate) {
		return 0;
	}
	
	public synchronized void addJobListener(JobListener listener) {}
}
	
