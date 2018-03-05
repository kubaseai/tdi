package com.tibco.pe.core;

import com.tibco.pe.model.ActivityDefinition;


public interface Task {

	public abstract String eval(Job processcontext);
	
	public abstract Workflow getWorkflow();

	public abstract Object getOutputBinds();

	public abstract Object getActivity();
	
	public abstract boolean isUsingSeparateThread();
	
	public abstract Object getTransactionScope();
	
	public abstract String getName();

	public static final String STAY_HERE = new String("STAY_HERE");
	public static final String ERROR = new String("ERROR");
	public static final String DEAD = new String("DEAD");
	public static final String CONTINUE = new String("CONTINUE");
	public static final String DEBUG = new String("DEBUG");
	public static final String BYPASS = new String("BYPASS");
	public static final String NULL_REPLY = new String("NullReply");
	
	public abstract void setActivity(ActivityDefinition paramActivityDefinition);

}