package com.tibco.objectrepo.object;

import java.util.Iterator;

import com.tibco.objectrepo.vfile.VFileFactory;
import tdi.core.CompileTimeStub;

@CompileTimeStub
public class ObjectProvider {
	
	private final static Iterator<VFileFactory> EMPTY_ITERATOR = new Iterator<VFileFactory>() {		
		public void remove() {}		
		public VFileFactory next() {			
			return null;
		}		
		public boolean hasNext() {			
			return false;
		}
	};

	public Iterator<VFileFactory> getAllProjects() {
		return EMPTY_ITERATOR;
	}

	public void unregisterProject(VFileFactory factory) {
	}
}
