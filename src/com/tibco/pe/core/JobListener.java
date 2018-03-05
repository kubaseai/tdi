package com.tibco.pe.core;

import com.tibco.xml.datamodel.XiNode;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public interface JobListener {

	public abstract boolean afterExecution(JobEvent je);
	public abstract boolean beforeExecution(JobEvent je);
	public abstract void errorLogged(String s, String s1, XiNode xinode, long l);
	public abstract void processCalled(JobEvent je, String s, boolean flag);
	public abstract void stateChanged(boolean active, long l);
	public abstract void trackAborted(String s, long l, int i);
	public abstract void transitionEvaluated(String s, String dst, String name, boolean b, long l, int i);
	public abstract boolean wantsActivityInput();
}
