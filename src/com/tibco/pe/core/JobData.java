package com.tibco.pe.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public final class JobData {

	public String wf;
	
	public long jid;
	
	public JobData(String taskName, Job job) {}
	
	public void fillJob(Job job, JobPool pool) {}
	
	private void writeObject(ObjectOutputStream stream) throws IOException {}
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {}
	
}
