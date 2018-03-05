package com.tibco.pe.core;

import java.util.Iterator;

import com.tibco.bw.store.RepoAgent;
import com.tibco.pe.model.ProcessModel;
import com.tibco.share.util.Trace;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public class WorkflowLoader {

	public WorkflowLoader(RepoAgent ra, Trace debugTrace,
			Trace startTrace, JobPool jobPool) {
		throw new RuntimeException(getClass().getName()+" is only compile time stub");	
	}

	public void loadWorkflows() {
		throw new RuntimeException(getClass().getName()+" is only compile time stub");		
	}
	
	public Iterator<?> getWorkflows() {
		throw new RuntimeException(getClass().getName()+" is only compile time stub");	
	}
	
	public void listWorkflows() {}

	public Workflow loadWorkflow(ProcessModel pm) {		
		return null;
	}

	public void removeAll() {		
	}
}
