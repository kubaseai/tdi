package com.tibco.pe.core;

public class JobContainer {
	public Job job;
	public JobContainer(Job job) {
		this.job = job;
	}
	public void setHibernated(boolean b) {
		job.ishibernated = b;
	}
}
