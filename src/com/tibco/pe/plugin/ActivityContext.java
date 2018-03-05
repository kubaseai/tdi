package com.tibco.pe.plugin;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public interface ActivityContext extends Tracer {

	public abstract String getProcessModelName();

	public abstract String getDescription();

	public abstract String getTraceSource();

	public abstract void useSeparateThread(String s, int i, long l)
			throws ActivityException;

	public abstract String getName();

	public abstract Object getThreadLocalVariable(boolean flag)
			throws Exception;

	public abstract void setThreadLocalVariable(Object obj, boolean flag)
			throws Exception;

	public abstract Object getThreadLocalVariable(String s) throws Exception;

}
