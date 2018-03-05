/*
 * Copyright (c) 2010-2011 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

import tdi.core.JobEventStats;

public class VoidTransport extends Transport {

	@Override
	public Transport init() {
		return this;
	}

	@Override
	public boolean send(AtomicReference<byte[][]> req, LinkedList<JobEventStats> events) {
		req.set(new byte[][] { new byte[] {}, new byte[] { 0 } });
		return true;
	}

	@Override
	public boolean send(byte[] data) {
		return true;
	}

	@Override
	protected void sendEvents(LinkedList<JobEventStats> events) throws Exception {}

	@Override
	public void uploadProcessesIfNeeded(
			HashMap<String, String[]> retrieveProcessFiles) throws Exception {			
	}
	
	@Override
	protected LinkedList<JobEventStats> retrieveEvents() throws Exception {
		return new LinkedList<JobEventStats>();
	}
}
