/*
 * Copyright (c) 2010-2011 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.core.activities;

import tdi.core.JobEventStats;
import tdi.core.NodeTextExtractor;

import com.tibco.bw.store.RepoAgent;
import com.tibco.xml.datamodel.XiNode;

public class JMSActivity extends Activity {

	@Override
	public void gatherMetrics(JobEventStats jl, XiNode data, XiNode cfg, XiNode input, RepoAgent ra) {
		super.gatherMetrics(jl, data, cfg, input, ra);
		
		if (data!=null) {
			XiNode activity = data.hasChildNodes() ? data.getFirstChild() : null;
			if (activity!=null) {
				XiNode nd = activity.hasChildNodes() ? activity.getFirstChild() : null;
				while (nd!=null) {
					if ("JMSHeaders".equals( nd.getName().getLocalName() )) {
						XiNode nd0 = nd.hasChildNodes() ? nd.getFirstChild() : null;
						while (nd0!=null) {
							String name = nd0.getName().getLocalName();
							String val = NodeTextExtractor.getText(nd0);
							jl.getMetrics().put(name, val);
							nd0 = nd0.hasNextSibling() ? nd0.getNextSibling() : null;
						}
						break;
					}
					nd = nd.hasNextSibling() ? nd.getNextSibling() : null;
				}
			}
		}
		
		if (cfg!=null) {
			XiNode config = data.hasChildNodes() ? data.getFirstChild() : null;
			if (config!=null) {
				XiNode nd = config.hasChildNodes() ? config.getFirstChild() : null;
				while (nd!=null) {
					if ("SessionAttributes".equals( nd.getName().getLocalName() )) {
						XiNode nd0 = nd.hasChildNodes() ? nd.getFirstChild() : null;
						while (nd0!=null) {
							String name = nd0.getName().getLocalName();
							String val = NodeTextExtractor.getText(nd0);
							jl.getMetrics().put(name, val);
							nd0 = nd0.hasNextSibling() ? nd0.getNextSibling() : null;
						}
					}
					else if ("ConnectionReference".equals( nd.getName().getLocalName() )) {
						;
					}
					nd = nd.hasNextSibling() ? nd.getNextSibling() : null;
				}
			}
		}		
	}
}
