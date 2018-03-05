package com.tibco.pe.plugin;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public interface Tracer {

	public abstract boolean isTracing();

	public abstract void trace(String s);	
}
