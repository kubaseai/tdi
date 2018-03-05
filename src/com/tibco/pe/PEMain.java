package com.tibco.pe;

import java.util.Properties;

import com.tibco.objectrepo.object.ObjectProvider;
import com.tibco.objectrepo.vfile.VFileFactory;
import com.tibco.pe.core.JobPoolListener;
import com.tibco.sdk.tools.MFileSink;

import tdi.core.CompileTimeStub;

@CompileTimeStub
public class PEMain {
	
	public PEMain(Properties properties) {}

	public PEMain(String[] as) {}

	public PEMain() {}

	public PEMain(Properties properties, MFileSink mfilesink) {}

	public PEMain(VFileFactory vfilefactory, ObjectProvider objectprovider,
			ClassLoader classloader) {}

	protected static boolean isMain = false;
	
	protected void addJobPoolListener(JobPoolListener makeJobPoolListener) {}

	public void start() throws Exception {}	
}
