package com.tibco.pe.core;

import java.util.Iterator;

import tdi.core.CompileTimeStub;

import com.tibco.pe.model.ProcessReport;

@CompileTimeStub
public class WorkflowImpl implements Workflow {
	
	String j = "";
	
	public WorkflowImpl(String s) {
		j = s;
	}

	public ProcessReport getProcessReport() {		
		return null;
	}

	public ProcessStarter getStarter() {
		return null;
	}

	public boolean isScore() {
		return true;
	}

	public String getName() {
		return j;
	}

	public void uninit() {		
	}

	@SuppressWarnings("rawtypes")
	public Iterator getTasks() {
		return null;
	}
}
