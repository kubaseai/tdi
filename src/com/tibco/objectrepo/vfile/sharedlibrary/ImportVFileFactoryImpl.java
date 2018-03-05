package com.tibco.objectrepo.vfile.sharedlibrary;

import com.tibco.objectrepo.vfile.VFileDirectory;
import com.tibco.objectrepo.vfile.VFileFactory;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public class ImportVFileFactoryImpl implements VFileFactory {

	public ImportVFileFactoryImpl(VFileFactory localVFileFactory) {		
	}

	public VFileDirectory getRootDirectory() {		
		return null;
	}
}
