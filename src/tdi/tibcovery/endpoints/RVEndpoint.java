/*
 * Copyright (c) 2010-2011 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * By using this file and API you are obligated to use GPL licence
 * for your code.
 */

package tdi.tibcovery.endpoints;

public class RVEndpoint extends Endpoint {
	
	

	private String scheduleHeartbeat = null;
	private String scheduleActivation = null;
	private String daemon = null;
	private String network = null;
	private String service = null;
	private String cmName = null;
	private String ledgerFile = null;
	private String syncLedger = null;
	private String requireOld = null;
	private String operationTimeout = null;
	private String subject = null;
	private String listener = null;
	
	public RVEndpoint() {}

	@Override
	public Endpoint deepClone() {
		RVEndpoint rv = new RVEndpoint();
		rv.destinations.addAll(destinations);
		rv.scheduleActivation = scheduleActivation;
		rv.scheduleHeartbeat = scheduleHeartbeat;
		rv.daemon = daemon;
		rv.network = network;
		rv.service = service;
		rv.cmName = cmName;
		rv.ledgerFile = ledgerFile;
		rv.syncLedger = syncLedger;
		rv.requireOld = requireOld;
		rv.operationTimeout = operationTimeout;
		rv.subject = subject;
		rv.listener = listener;
		return rv;
	}

	@Override
	public String getGroupingKey() {
		return subject!=null ? subject : (listener!=null ? listener : "<empty-subject>");
	}

	public String getScheduleHeartbeat() {
		return scheduleHeartbeat;
	}

	public void setScheduleHeartbeat(String scheduleHeartbeat) {
		this.scheduleHeartbeat = scheduleHeartbeat;
	}

	public String getScheduleActivation() {
		return scheduleActivation;
	}

	public void setScheduleActivation(String scheduleActivation) {
		this.scheduleActivation = scheduleActivation;
	}

	public String getDaemon() {
		return daemon;
	}

	public void setDaemon(String daemon) {
		this.daemon = daemon;
	}

	public String getNetwork() {
		return network;
	}

	public void setNetwork(String network) {
		this.network = network;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getCmName() {
		return cmName;
	}

	public void setCmName(String cmName) {
		this.cmName = cmName;
	}

	public String getLedgerFile() {
		return ledgerFile;
	}

	public void setLedgerFile(String ledgerFile) {
		this.ledgerFile = ledgerFile;
	}

	public String getSyncLedger() {
		return syncLedger;
	}

	public void setSyncLedger(String syncLedger) {
		this.syncLedger = syncLedger;
	}

	public String getRequireOld() {
		return requireOld;
	}

	public void setRequireOld(String requireOld) {
		this.requireOld = requireOld;
	}

	public String getOperationTimeout() {
		return operationTimeout;
	}

	public void setOperationTimeout(String operationTimeout) {
		this.operationTimeout = operationTimeout;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
		this.destinations.add(subject);
	}

	public void setListener(String listener) {
		this.listener = listener;
		if (listener!=null)
			this.destinations.add("listener:"+listener);		
	}
	
	@Override
	public String description() {		
		StringBuilder builder = new StringBuilder();
		builder.append("RVEndpoint [subject=");
		builder.append(subject);
		builder.append(",\r\nlistener=");
		builder.append(listener);
		builder.append(",\r\ndaemon=");
		builder.append(daemon);
		builder.append(", network=");
		builder.append(network);
		builder.append(", service=");
		builder.append(service);
		builder.append(",\r\ncmName=");
		builder.append(cmName);
		builder.append(", ledgerFile=");
		builder.append(ledgerFile);
		builder.append(",\r\nsyncLedger=");
		builder.append(syncLedger);
		builder.append(", requireOld=");
		builder.append(requireOld);
		builder.append(", operationTimeout=");
		builder.append(operationTimeout);
		builder.append(",\r\nscheduleActivation=");
		builder.append(scheduleActivation);
		builder.append(",scheduleHeartbeat=");
		builder.append(scheduleHeartbeat);		
		builder.append("]");
		return builder.toString();		
	}	
}
