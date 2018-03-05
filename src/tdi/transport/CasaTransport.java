/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.CfDef;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.KsDef;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import tdi.core.JobEventStats;
import tdi.core.Logger;

public class CasaTransport extends Transport {
	
	private final static String KS_TDI = "tdi";
	private static TTransport tr = null;
	private static Cassandra.Client client = null;
	private static boolean firstOpen = true;
	
	@Override
	public Transport init() {
		setRemoteAddress(remoteAddress);
		return this;
	}
	
	private final static void open() throws Exception {
		if (firstOpen)
			Logger.getInstance().debug("Connection to Long Term NoSQL Storage");
		tr.open();
		if (firstOpen) {
			firstOpen = false;
			KsDef ks = null;
			try {
				ks = client.describe_keyspace(KS_TDI);
			}
			catch (NotFoundException nfe) {
				ks = new KsDef();		
				ks.setName(KS_TDI);
				ks.setStrategy_class("org.apache.cassandra.locator.SimpleStrategy");
				ks.addToCf_defs(new CfDef(KS_TDI, "events"));
				ks.addToCf_defs(new CfDef(KS_TDI, "usage"));
				ks.addToCf_defs(new CfDef(KS_TDI, "params"));
				ks.setStrategy_options(Collections.singletonMap("replication_factor", "1"));
				client.system_add_keyspace(ks);
			}
		}
		client.set_keyspace(KS_TDI);
	}
	
	private final static void sendEventsAndUsage(List<JobEventStats> list) throws Exception {
		try {
			open();
			for (JobEventStats ev : list) {
				CasaColumnContainer ctEvent = CasaAdapter.getColumnContainer(ev);
				for (Column c : ctEvent.getColumns())
					client.insert(ctEvent.getRowId(), ctEvent.getColumnParent(), c, ConsistencyLevel.ONE);
			}
			CasaColumnContainer ctUsage = CasaAdapter.getColumnContainer("usage");
			for (Column c : ctUsage.getColumns())
				client.insert(ctUsage.getRowId(), ctUsage.getColumnParent(), c, ConsistencyLevel.ONE);
		}
		finally {
			close();
		}
	}

	private static void close() {
		try {
			tr.close();
		}
		catch (Exception e) {}		
	}

	public static void setRemoteAddress(String remoteAddress) {
		if (remoteAddress!=null && remoteAddress.startsWith("cassandra:"))
			remoteAddress = remoteAddress.substring(10);
		String[] hp = Transport.parseHostPort(remoteAddress, 9160);
		tr = new TFramedTransport(new TSocket(hp[0], Integer.valueOf(hp[1])));	
		client = new Cassandra.Client(new TBinaryProtocol(tr));
	}

	@Override
	protected void sendEvents(LinkedList<JobEventStats> events) throws Exception {
		sendEventsAndUsage(events);		
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
