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

public class SAPEndpoint extends AdapterEndpoint {
	
	private String maxConnections = null;
	private String systemNumber = null;
	private String appServer = null;
	
	private String user = null;
	private String password = null;
	private String language = null;
	private String clientNumber = null;
	
	private String gatewayService = null;
	private String programID = null;
	private String gatewayHost = null;
	
	protected SAPEndpoint() {}
	
	public SAPEndpoint(AdapterEndpoint ae) {
		this.adapterService = ae.adapterService;
		this.setOperation(ae.operation);
		this.transportType = ae.transportType;
		this.setFromProps(ae.getProps());
	}

	@Override
	public void setOperation(String operation) {
		this.operation = operation;
		if (operation!=null)
			this.destinations.add("sap '"+operation+"'");
	}
	
	@Override
	public Endpoint deepClone() {
		SAPEndpoint sap = new SAPEndpoint(this);
		sap.gatewayHost = gatewayHost;
		sap.gatewayService = gatewayService;
		sap.programID = programID;
		sap.maxConnections = maxConnections;
		sap.systemNumber = systemNumber;
		sap.appServer = appServer;
		sap.user = user;
		sap.password = password;
		sap.language = language;
		sap.clientNumber = clientNumber;
		return sap;
	}

	public String getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(String maxConnections) {
		this.maxConnections = maxConnections;
	}

	public String getSystemNumber() {
		return systemNumber;
	}

	public void setSystemNumber(String systemNumber) {
		this.systemNumber = systemNumber;
	}

	public String getAppServer() {
		return appServer;
	}

	public void setAppServer(String appServer) {
		this.appServer = appServer;
	}

	public String getGatewayService() {
		return gatewayService;
	}

	public void setGatewayService(String gatewayService) {
		this.gatewayService = gatewayService;
	}

	public String getProgramID() {
		return programID;
	}

	public void setProgramID(String programID) {
		this.programID = programID;
	}

	public String getGatewayHost() {
		return gatewayHost;
	}

	public void setGatewayHost(String gatewayHost) {
		this.gatewayHost = gatewayHost;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getClientNumber() {
		return clientNumber;
	}

	public void setClientNumber(String clientNumber) {
		this.clientNumber = clientNumber;
	}	
	
	@Override
	public String description() {
		StringBuilder builder = new StringBuilder();
		builder.append("SAPEndpoint [maxConnections=");
		builder.append(maxConnections);
		builder.append(", systemNumber=");
		builder.append(systemNumber);
		builder.append(", appServer=");
		builder.append(appServer);
		builder.append(",\r\nuser=");
		builder.append(user);
		builder.append(", password=");
		builder.append(password);
		builder.append(", language=");
		builder.append(language);
		builder.append(",\r\nclientNumber=");
		builder.append(clientNumber);
		builder.append(", gatewayService=");
		builder.append(gatewayService);
		builder.append(",\r\nprogramID=");
		builder.append(programID);
		builder.append(", gatewayHost=");
		builder.append(gatewayHost);
		builder.append("]\r\n");
		builder.append(super.description());
		return builder.toString();
	}
}
