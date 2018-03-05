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

public class JDBCEndpoint extends Endpoint {
	
	private String timeout = null;
	private String statement = null;
	private String maxRows = null;
	private String schema = null;
	private String pkg = null;
	private String procedureName = null;
	private String driver= null;
	private String maxConnections= null;
	private String loginTimeout= null;
	private String url= null;
	private String user= null;
	private String password = null;
	
	public JDBCEndpoint() {}
	
	@Override
	public Endpoint deepClone() {
		JDBCEndpoint jdbc = new JDBCEndpoint();
		jdbc.setDriver(getDriver());
		jdbc.setLoginTimeout(getLoginTimeout());
		jdbc.setMaxConnections(getMaxConnections());
		jdbc.setMaxRows(getMaxRows());
		jdbc.setProcedureName(getProcedureName());
		jdbc.setSchema(getSchema());
		jdbc.setStatement(getStatement());
		jdbc.setTimeout(getTimeout());
		jdbc.setUrl(getUrl());
		jdbc.setUser(getUser());
		jdbc.setPackage(getPackage());
		jdbc.setPassword(getPassword());
		jdbc.destinations.addAll(destinations);
		return jdbc;
	}

	public String getTimeout() {
		return timeout;
	}

	public String getStatement() {
		return statement;
	}

	public String getMaxRows() {
		return maxRows;
	}

	public String getSchema() {
		return schema;
	}

	public String getProcedureName() {
		return procedureName;
	}

	public void setTimeout(String timeout) {
		this.timeout = timeout;		
	}

	public void setStatement(String statement) {
		this.statement = statement;		
	}

	public void setMaxRows(String maxRows) {
		this.maxRows = maxRows;		
	}

	public void setSchema(String schema) {
		this.schema = schema;		
	}

	public void setProcedureName(String procName) {
		this.procedureName = procName;		
	}
	
	public void setPackage(String pkg) {
		this.pkg = pkg;
	}
	
	public String getPackage() {
		return this.pkg;
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(String maxConnections) {
		this.maxConnections = maxConnections;
	}

	public String getLoginTimeout() {
		return loginTimeout;
	}

	public void setLoginTimeout(String loginTimeout) {
		this.loginTimeout = loginTimeout;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
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

	@Override
	public String getGroupingKey() {
		return (procedureName!=null) ? (schema!=null ? schema+"." : "")+(pkg!=null ? pkg+"." : "")+procedureName 
				: (statement!=null ? statement : "<empty-statement>" );
	}

	public void setDestination() {
		destinations.add(getGroupingKey());		
	}

	@Override
	public String description() {
		StringBuilder builder = new StringBuilder();
		builder.append("JDBCEndpoint [url=");
		builder.append(url);
		builder.append(",\r\nstatement=");
		builder.append(statement);
		builder.append(",\r\nmaxRows=");
		builder.append(maxRows);
		builder.append(", schema=");
		builder.append(schema);
		builder.append(", package=");
		builder.append(pkg);
		builder.append(", procedureName=");
		builder.append(procedureName);
		builder.append(",\r\ndriver=");
		builder.append(driver);
		builder.append(",\r\nmaxConnections=");
		builder.append(maxConnections);
		builder.append(", timeout=");
		builder.append(timeout);
		builder.append(", loginTimeout=");
		builder.append(loginTimeout);
		builder.append(",\r\nuser=");
		builder.append(user);		
		builder.append(", password=");
		builder.append(password);
		builder.append("]");
		return builder.toString();
	}
}
