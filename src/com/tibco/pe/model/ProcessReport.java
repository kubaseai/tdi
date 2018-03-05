package com.tibco.pe.model;

import com.tibco.bw.store.RepoAgent;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public class ProcessReport {

	public ActivityReport getActivity(String an) {
		throw new RuntimeException(this.getClass().getName()+" is only compile time stub");
	}

	public RepoAgent getRepoAgent() {
		throw new RuntimeException(this.getClass().getName()+" is only compile time stub");
	}

}
