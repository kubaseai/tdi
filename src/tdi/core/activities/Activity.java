/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.core.activities;

import java.util.HashMap;

import org.w3c.dom.Node;

import tdi.core.JobEventStats;
import tdi.core.XiNodeWrapper;
import tdi.tibcovery.common.DOMHelper;
import tdi.tibcovery.endpoints.Endpoint;
import tdi.tibcovery.endpoints.JMSEndpoint;
import tdi.tibcovery.endpoints.SOAPEndpoint;

import com.tibco.bw.store.RepoAgent;
import com.tibco.xml.datamodel.XiNode;

public class Activity {
	
	private static HashMap<String, Activity> map = new HashMap<String, Activity>();
	static {
		map.put("com.tibco.plugin.jms.JMSQueueEventSource", new JMSActivity());
		map.put("com.tibco.plugin.jms.JMSTopicEventSource", new JMSActivity());
		map.put("com.tibco.plugin.mapper.MapperActivity", new MapperActivity());
		map.put("", new Activity());
	}

	public static Activity forClass(String className) {
		Activity a = map.get(className);
		return a!=null ? a : map.get("");
	}

	public static void gatherErrorMetrics(JobEventStats jl, XiNode er) {
		jl.incErrorCount(er);
		if (jl.hasOomError())
			throw new OutOfMemoryError("TDI detected OOM in process "+jl.getProcessName());
	}

	public void gatherMetrics(JobEventStats jl, XiNode data, XiNode cfg, XiNode input, RepoAgent ra) {
		/** implemented also by subclasses **/	
		if (input!=null)
			MarkerDefinition.processInput(jl, input);
		if (data!=null && data!=input) // do we want to store twice starters?
			MarkerDefinition.processData(jl, data);
	}

	public Endpoint gatherTopology(JobEventStats jl, XiNode data, XiNode cfg,
			XiNode input, RepoAgent ra) {
		boolean isStarter = ProcessStarterInfo.isStarter(jl.getProcessName(), jl.getActivityName());
		Node cfgNode = new XiNodeWrapper(cfg);
		String lastType = jl.getLastActivityClassName();
		String classConfig = DOMHelper.getNodeValueNs(cfgNode, "class");
		Endpoint ep = null;
		if (lastType!=null && lastType.startsWith("com.tibco.plugin.jms.JMSQueue")) {
			ep = new JMSEndpoint(cfgNode, new XiNodeWrapper(input));				
		}	
		else if ("com.tibco.plugin.soap.SOAPSendReceiveActivity".equals(lastType)) {
			ep = new SOAPEndpoint(cfgNode, new XiNodeWrapper(input));			
		}
		else if ("com.tibco.bw.service.serviceAgent.ServiceServiceAgent".equals(classConfig)) {
			ep = new SOAPEndpoint(cfgNode);
		}
		if (ep!=null) {
			ep.setStarter(isStarter);			
		}
		return ep;
	}
}
