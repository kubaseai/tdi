package com.tibco.pe.plugin;

import tdi.core.CompileTimeStub;

import com.tibco.xml.datamodel.XiNode;

@CompileTimeStub
public interface ConfigModel {
    public abstract XiNode getConfigParms();
}
