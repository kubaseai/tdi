package com.tibco.pe.dm;

import com.tibco.pe.core.JobData;

public interface JobDataManager {

	public abstract JobData load(long l, String s, boolean flag)
			throws Exception;	
}
