/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.util.Map;
import java.util.Map.Entry;

import tdi.core.JobEventStats;
import tdi.core.activities.JVMStats;
import tdi.transport.Transport;

public class CasaAdapter {
	
	public static CasaColumnContainer getColumnContainer(JobEventStats ev) {
		CasaColumnContainer container = new CasaColumnContainer("events", ev.getStart());
		container.addIfNotEmpty("host", Transport.getHostname());
		for (Entry<String,Object> en : ev.getFields().entrySet()) {
			container.addIfNotEmpty(en.getKey(), en.getValue());
		}		
		
		for (Map.Entry<String,String> en : ev.getMetrics().entrySet()) {
			container.addIfNotEmpty(en.getKey(), en.getValue());
		}
		
		StringBuffer rowId = new StringBuffer("ST30M_");
		rowId.append(ev.getStart()/1800000).append("_");
		
		String[] mks = ev.getGlobalMarkers();
		boolean hasMarker = false;
		for (int i=0; i < mks.length; i++) {
			if (mks[i]!=null) {
				container.addIfNotEmpty("mk"+(i+1), mks[i]);
				if (!hasMarker) {
					rowId.append("MK"+(i+1)).append("_").append(mks[i]).append("_");
					hasMarker = true;
				}
			}
		}
		if (!hasMarker)
			rowId.append("MK0_0_");
		rowId.append("ST_").append(ev.getStart()).append("_");
		rowId.append("REPO_").append(ev.getRepoName()).append("_");
		rowId.append("JOB_").append(ev.getJobId());
		container.setRowId(rowId.toString());
		return container;
	}

	public static CasaColumnContainer getColumnContainer(String kind) {
		CasaColumnContainer container = null;
		long ts = System.currentTimeMillis();
		if ("usage".equals(kind)) {
			double cpu = JVMStats.getJvmCpuUsage();
	        double mem = JVMStats.getPercentMemUsage();
	        double gc = JVMStats.getPercentGcUsage();
	        container = new CasaColumnContainer("usage", ts);
	        String host = Transport.getHostname();
			String repo = JobEventStats.getRepositoryName();
	        container.addIfNotEmpty("host", host);
			container.addIfNotEmpty("repo", repo);
	        container.addIfNotEmpty("cpu", cpu);
	        container.addIfNotEmpty("mem", mem);
			container.addIfNotEmpty("gc", gc);
	        container.addIfNotEmpty("st", ts);
	        container.addIfNotEmpty("st3m", ts/180000);
			container.addIfNotEmpty("st30m", ts/1800000);
			container.addIfNotEmpty("st4h", ts/14400000);
			container.addIfNotEmpty("st1d", ts/86400000);	
			container.setRowId("HOST_"+host+"_REPO_"+repo+"_ST3M_"+(ts/180000)+"_"+ts);
		}
		else if ("params".equals(kind)) {
			container = new CasaColumnContainer("usage", ts);
			String host = Transport.getHostname();
			String repo = JobEventStats.getRepositoryName();
	        container.addIfNotEmpty("host", host);
			container.addIfNotEmpty("repo", repo);
			container.addIfNotEmpty("param", "NEEDS_CONFIG");
	        container.addIfNotEmpty("value", "");
	        container.setRowId("HOST_"+host+"_REPO_"+repo+"_"+ts);
		}
		else
			container = new CasaColumnContainer("none", 0);
		return container;
	}
}
