/*
 * Copyright (c) 2010-2014 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.core.activities;

import java.util.HashMap;

import tdi.core.JVMHelper;
import tdi.core.JobEventStats;
import tdi.core.Logger;
import tdi.core.PEProc;
import tdi.tibcovery.endpoints.Endpoint;

import com.tibco.bw.store.RepoAgent;
import com.tibco.pe.core.EngineHelper;
import com.tibco.xml.datamodel.XiNode;

public class Analyzer {
	
	private final static HashMap<String, Endpoint> epCache = new HashMap<String, Endpoint>();

	public static void analyzeProcess(JobEventStats jl, String className, XiNode data, XiNode cfg, XiNode input) {
		try {
			RepoAgent ra = EngineHelper.getRepoAgent();
			Activity a = Activity.forClass(className);
			a.gatherMetrics(jl, data, cfg, input, ra);
			Endpoint ep = a.gatherTopology(jl, data, cfg, input, ra);	
			if (ep!=null) {
				if (epCache.put(ep.description(), ep) == null) {
					PEProc.addEvent(JobEventStats.createPushPopEvent(0, null, "ep", JobEventStats.getRepositoryName(), ep.toXml()));
				}
			}
		}
		catch (OutOfMemoryError oom) {
			JVMHelper.oomExit(oom);
		}
		catch (Exception e) {
			Logger.getInstance().debug("analyzeProcess ERROR",e);
		}
	}

	public static void analyzeError(JobEventStats jl, XiNode er) {
		try {
			Activity.gatherErrorMetrics(jl, er);	
		}
		catch (OutOfMemoryError oom) {
			JVMHelper.oomExit(oom);
		}
		catch (Exception e) {
			Logger.getInstance().debug("analyzeError ERROR",e);
		}
	}
}
