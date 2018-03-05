package com.tibco.pe.plugin;

import com.tibco.xml.datamodel.XiNode;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public interface DataModel extends ConfigModel {
	public String getClassName();
	public XiNode getConfigParms();
}
