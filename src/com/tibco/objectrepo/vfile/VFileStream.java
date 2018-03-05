package com.tibco.objectrepo.vfile;

import java.io.InputStream;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public interface VFileStream extends VFile {

	public String getType();	

	public InputStream getInputStream();
}
