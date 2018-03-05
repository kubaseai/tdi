/*
 * Copyright (c) 2010-2011 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;

import tdi.core.JobEventStats;

public class HBaseTransport extends Transport {

	private HTable ht = null;
	
	@Override
	public Transport init() {
		Configuration cfg = HBaseConfiguration.create();
		try {
			ht = new HTable(cfg, "events");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public void sendEvents(LinkedList<JobEventStats> events) throws IOException {
		LinkedList<Put> list = new LinkedList<Put>();
		byte[] id = (Transport.getHostname()+"_"+events.get(0).getRepoName()+"_"+System.currentTimeMillis()+"").getBytes();
		for (JobEventStats je : events) {
			Put put = new Put(id);
			for (Map.Entry<String, Object> en : je.getFields().entrySet()) {
				put.add(en.getKey().getBytes(), null, en.getValue()!=null ?
					en.getValue().toString().getBytes() : null);
			}
			list.add(put);
		}
		ht.put(list);
		ht.flushCommits();		
	}

	public void finalize() {
		if (ht!=null) {
			try {
				ht.close();
			}
			catch (IOException e) {}
		}
	}
	
	public final static void main(String[] args) throws IOException {
		HBaseTransport tr = new HBaseTransport();
		LinkedList<JobEventStats> list = new LinkedList<JobEventStats>();
		list.add(new JobEventStats(1000));
		tr.init();
		tr.sendEvents(list);
	}

	@Override
	public void uploadProcessesIfNeeded(
			HashMap<String, String[]> retrieveProcessFiles) throws Exception {
		/** FIXME: implement */		
	}
	
	@Override
	protected LinkedList<JobEventStats> retrieveEvents() throws Exception {
		/** FIXME: implement */	
		return new LinkedList<JobEventStats>();
	}
}
