package com.tibco.pe.core;

import com.tibco.bw.store.RepoAgent;
import com.tibco.objectrepo.object.ObjectProvider;
import com.tibco.objectrepo.vfile.VFileFactory;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public class RepoAgentImpl implements RepoAgent {

	public void clearCache() {
		throw new RuntimeException(RepoAgentImpl.class.getName()+" is only compile time stub");		
	}

	public String getProjectName() {
		throw new RuntimeException(RepoAgentImpl.class.getName()+" is only compile time stub");	
	}

	public ObjectProvider getObjectProvider() {
		throw new RuntimeException(RepoAgentImpl.class.getName()+" is only compile time stub");	
	}

	public String getAbsoluteURIFromProjectRelativeURI(String string) {
		throw new RuntimeException(RepoAgentImpl.class.getName()+" is only compile time stub");	
	}

	public VFileFactory getVFileFactory() {
		throw new RuntimeException(RepoAgentImpl.class.getName()+" is only compile time stub");	
	}
}
