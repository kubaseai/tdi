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

import java.util.Collections;
import java.util.Map;

public class AdapterEndpoint extends Endpoint {
	
	protected String operation = null;
	protected String adapterService =  null;
	protected String transportType = null;
	
	public AdapterEndpoint() {}
			
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
		if (operation!=null)
			this.destinations.add("adapter '"+operation+"'");
	}
	public String getAdapterService() {
		return adapterService;
	}
	public void setAdapterService(String adapterService) {
		this.adapterService = adapterService;
	}
	public String getTransportType() {
		return transportType;
	}
	public void setTransportType(String transportType) {
		this.transportType = transportType;
	}
	public void setFromProps(Map<String, String> props) {		
		String t = props.get("transportType");
		if ("jmsQueue".equals(t)) {
			JMSEndpoint jms = new JMSEndpoint();
			String queue = props.get("jmsQueueSessionQueue");
			if (queue!=null)
				jms.destinations.add(queue);
			jms.props.put("QueueFactoryName", props.get("jmsQueueSessionConnectionFactory"));
			jms.props.put("username", props.get("jmsSessionUsername"));
			jms.props.put("password", props.get("jmsSessionPassword"));
			jms.props.put("ProviderURL", props.get("jmsSessionProviderURL"));
			internalEndpoint = jms;			
		}		
		else if ("jmsTopic".equals(t)) {
			JMSEndpoint jms = new JMSEndpoint();
			String queue = props.get("jmsTopicSessionTopic");
			if (queue!=null)
				jms.destinations.add(queue);
			jms.props.put("TopicFactoryName", props.get("jmsTopicSessionConnectionFactory"));
			jms.props.put("username", props.get("jmsSessionUsername"));
			jms.props.put("password", props.get("jmsSessionPassword"));
			jms.props.put("ProviderURL", props.get("jmsSessionProviderURL"));
			internalEndpoint = jms;
		}
		else if (t!=null && t.startsWith("rv")) {			
			RVEndpoint rv = new RVEndpoint();
			rv.setCmName(props.get("rvCmSessionName"));
			rv.setService(props.get("rvSessionService"));
			rv.setNetwork(props.get("rvSessionNetwork"));
			rv.setDaemon(props.get("rvSessionDaemon"));
			rv.setLedgerFile(props.get("rvCmSessionLedgerFile"));
			rv.setSyncLedger(props.get("rvCmSessionSyncLedger"));
			rv.setRequireOld(props.get("rvCmSessionRequireOldMessages"));
			rv.setOperationTimeout(props.get("rvCmSessionDefaultTimeLimit"));
			rv.setSubject(props.get("rvSubject"));
			internalEndpoint = rv;
		}
		this.props.putAll(props);
		
	}
	@Override
	public Endpoint deepClone() {
		AdapterEndpoint ae = new AdapterEndpoint();
		ae.adapterService = adapterService;
		ae.setOperation(operation);
		ae.transportType = ae.transportType;
		ae.setFromProps(props);	
		ae.destinations.addAll(destinations);
		ae.internalEndpoint = (internalEndpoint!=null) ? internalEndpoint.deepClone() : null;
		return ae;
	}
	
	@Override
	public String getGroupingKey() {
		return adapterService!=null ? adapterService : "<empty-adapter-service>";
	}
	public Map<String, String> getProps() {
		return Collections.unmodifiableMap(props);
	}
	@Override
	public String description() {
		StringBuilder builder = new StringBuilder();
		builder.append("AdapterEndpoint [operation=");
		builder.append(operation);
		builder.append(",\r\nadapterService=");
		builder.append(adapterService);
		builder.append(", transportType=");
		builder.append(transportType);
		builder.append(",");
		for (Map.Entry<String,String> en : props.entrySet()) {
			builder.append("\r\n").append(en.getKey()).append("=").append(en.getValue());
		}		
		builder.append("]");
		return builder.toString();
	}
}
