package com.tibco.pe.core;

import java.util.Vector;

import com.tibco.bw.store.RepoAgent;
import com.tibco.share.util.Trace;
import com.tibco.xml.xdata.xpath.Variable;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public class Engine {

	protected static JobPool pool;
	public static Trace DEBUG_TRACER = null;
	public static EngineProperties props = null;
	
	public static RepoAgent getRepoAgent() {
		throw new RuntimeException(RepoAgent.class.getName()+" is only compile time stub");
	}

	public static Vector<Object> getModels() {
		return new Vector<Object>();
	}

	public static Variable getDeployedVarsVariable(String name) {
		return null;
	}
}
