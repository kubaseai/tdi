package com.tibco.bw.store;

import com.tibco.objectrepo.object.ObjectProvider;
import com.tibco.objectrepo.vfile.VFileFactory;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public interface RepoAgent {
	
	public abstract String getProjectName();

	public abstract ObjectProvider getObjectProvider();

	public abstract String getAbsoluteURIFromProjectRelativeURI(String string);

	public abstract VFileFactory getVFileFactory();

}
