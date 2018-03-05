package com.tibco.pe.core;

import java.util.EventListener;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public interface JobPoolListener extends EventListener {
	public abstract void jobAdded(JobPoolEvent jobpoolevent);
	public abstract void jobRemoved(JobPoolEvent jobpoolevent);
	public abstract void stateChanged(JobPoolEvent jobpoolevent);
}
