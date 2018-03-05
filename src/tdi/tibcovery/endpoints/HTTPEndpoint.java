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

public class HTTPEndpoint extends Endpoint {
	
	private String host;
	private String port;
	private String method;
	private String uri;
	private String timeout;
	private boolean useSSL;
	
	public HTTPEndpoint() {}
	
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getTimeout() {
		return timeout;
	}
	public void setTimeout(String timeout) {
		this.timeout = timeout;
	}
	public boolean isUseSSL() {
		return useSSL;
	}
	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}
	
	private String getUrl() {
		return (useSSL ? "https://" : "http://")+host+":"+port+((uri!=null && uri.startsWith("/")) ? uri : "/"+uri);
	}
	
	public void setDestination() {
		destinations.add(getUrl());		
	}
	
	@Override
	public Endpoint deepClone() {
		HTTPEndpoint http = new HTTPEndpoint();
		http.destinations.addAll(destinations);
		http.host = host;
		http.port = port;
		http.method = method;
		http.timeout = timeout;
		http.uri = uri;
		http.useSSL = useSSL;
		return http;
	}
	
	@Override
	public String getGroupingKey() {
		return getUrl();
	}
	@Override
	public String description() {
		StringBuilder builder = new StringBuilder();
		builder.append("HTTPEndpoint [host=");
		builder.append(host);
		builder.append(", port=");
		builder.append(port);
		builder.append(", method=");
		builder.append(method);
		builder.append(",\r\nuri=");
		builder.append(uri);
		builder.append(",\r\ntimeout=");
		builder.append(timeout);
		builder.append(", useSSL=");
		builder.append(useSSL);
		builder.append("]");
		return builder.toString();
	}	
}
