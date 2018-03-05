package com.tibco.pe.core;

import tdi.core.CompileTimeStub;

import com.tibco.pe.model.ProcessReport;

@CompileTimeStub
public interface Workflow {

	public abstract ProcessReport getProcessReport();
	public abstract ProcessStarter getStarter();	
	public abstract boolean isScore();
	public abstract String getName();
}