package com.tibco.pe.plugin;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public interface ActivityController {

	public abstract void setPending(long l);

	public abstract boolean setReady(Object obj) throws ActivityException;

	public abstract void setWaiting(String s, String s1, long l)
			throws ActivityException;

	public abstract void cancelWaiting(String s, String s1)
			throws ActivityException;
}

