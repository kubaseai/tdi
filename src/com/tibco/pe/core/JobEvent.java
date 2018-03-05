package com.tibco.pe.core;

import com.tibco.xml.datamodel.XiNode;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public class JobEvent {

	public String activityName;
	public String processName;
	public XiNode activityData;
	public String getActivityName() {
		return "---";
	}

}
