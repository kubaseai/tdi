/*
 * Copyright (c) 2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.util.HashMap;
import java.util.LinkedList;

import tdi.core.JobEventStats;
import tdi.core.Logger;
import tdi.core.PEProc;

public class MultiTransport extends Transport {
	
	private LinkedList<Transport> tr = new LinkedList<Transport>();

	public MultiTransport(String address) throws Exception {
		LinkedList<Exception> errors = new LinkedList<Exception>();
		for (String s : address.split("\n")) {
			String addrOverride = s.trim();
			if (addrOverride.startsWith("#"))
				continue;
			try {
				PEProc.setRuntimeProperty("TSI_ADDRESS", addrOverride);
				tr.add(Transport.createTransportForBootstrap(addrOverride));
			}
			catch (Exception e) {
				Logger.getInstance().debug("Transport ERROR",e);
				errors.add(e);
			}			
		}
		if (tr.size() == 0)
			throw new Exception(errors.toString());
	}

	@Override
	public Transport init() {
		Logger.getInstance().debug("Using MultiTransport with n="+tr.size());
		for (Transport t : tr)
			t.init();
		return this;
	}

	@Override
	protected void loadAutoConfig() {
		for (Transport t : tr)
			t.loadAutoConfig();
	}

	@Override
	protected void sendEvents(LinkedList<JobEventStats> events) throws Exception {
		for (Transport t : tr)
			t.sendEvents(events);		
	}

	@Override
	public void uploadProcessesIfNeeded(
			HashMap<String, String[]> retrieveProcessFiles) throws Exception {
		for (Transport t : tr)
			t.uploadProcessesIfNeeded(retrieveProcessFiles);		
	}
	
	@Override
	protected LinkedList<JobEventStats> retrieveEvents() throws Exception {
		LinkedList<JobEventStats> list = new LinkedList<JobEventStats>();
		for (Transport t : tr)
			list.addAll(t.retrieveEvents());
		return list;
	}
}
