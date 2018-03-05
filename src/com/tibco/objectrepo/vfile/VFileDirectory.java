package com.tibco.objectrepo.vfile;

import java.util.Iterator;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public interface VFileDirectory extends VFile {	
	
	public Iterator<VFile> getChildren();
}
