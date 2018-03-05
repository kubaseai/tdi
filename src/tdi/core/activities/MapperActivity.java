/*
 * Copyright (c) 2010-2011 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.core.activities;

import tdi.core.JobEventStats;

import com.tibco.bw.store.RepoAgent;
import com.tibco.xml.datamodel.XiNode;

public class MapperActivity extends Activity {
	
	@Override
	public void gatherMetrics(JobEventStats jl, XiNode data, XiNode cfg, XiNode input, RepoAgent ra) {
		/** implemented also by subclasses **/	
		if (input!=null)
			MarkerDefinition.processInput(jl, input);
		else if (data!=null)
			MarkerDefinition.processData(jl, data);
	}
}
