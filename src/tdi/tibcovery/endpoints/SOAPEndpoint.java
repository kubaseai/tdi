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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Node;

import tdi.tibcovery.common.DOMHelper;

public class SOAPEndpoint extends Endpoint {
	
	private String name = null;
	private HashMap<String,String> operations = new HashMap<String, String>();
	private String url;
	private String timeout = null;	
	
	public SOAPEndpoint() {}
	
	public SOAPEndpoint(Node cfg, Node input) {
		String operation = DOMHelper.getNodeValueNs(cfg, "operation");
		String soapAction = DOMHelper.getNodeValueNs(cfg, "soapAction");
		operations.put(operation, soapAction);
		url = DOMHelper.getNodeValueNs(cfg, "endpointURL");	
		if (url==null) {
			Node jmsTo =  DOMHelper.getElementByTagNs(cfg, "JMSTo", 0);
			if (jmsTo!=null)
				url = jmsTo.getNodeValue();
		}
		String timeoutType = DOMHelper.getNodeValueNs(cfg, "timeoutType");
		timeout = DOMHelper.getNodeValueNs(cfg, "timeoutType");
		name = DOMHelper.getNodeValueNs(cfg, "service");
		if (name!=null) {
			int p = name.indexOf(':');
			if (p!=-1)
				name = name.substring(p+1);
		}
				
		Node inputMessage = DOMHelper.getElementByTagNs(input, "inputMessage", 0);
		if (inputMessage!=null) {
			Node _configData = DOMHelper.getElementByTagNs(input, "_configData", 0);
			if (_configData!=null) {
				String epURL = DOMHelper.getNodeValueNs(_configData, "endpointURL");
				if (epURL==null)
					epURL = DOMHelper.getNodeValueNs(_configData, "JMSTargetDestination");
				if (epURL!=null)
					url = epURL;
				String tm = DOMHelper.getNodeValueNs(_configData, "timeout");
				if (tm!=null)
					timeout = tm;
			}
		}
		if ("MilliSeconds".equals(timeoutType)) {
			timeout = timeout + " ms";
		}
	}

	public SOAPEndpoint(Node cfgNode) {
		name = DOMHelper.getNodeValueNs(cfgNode, "name");
		Node bindings = DOMHelper.getElementByTagNs(cfgNode, "epBindings", 0);
		if (bindings!=null) {
			Node httpUri = DOMHelper.getElementByTagNs(bindings, "httpURI", 0);
			if (httpUri!=null)
				url = httpUri.getNodeValue();
			else {
				Node dest = DOMHelper.getElementByTagNs(bindings, "JMSTo", 0);
				Node destType =  DOMHelper.getElementByTagNs(bindings, "JMSDestinationType", 0);
				if (dest!=null && destType!=null) {
					url = destType.getNodeValue() + ": " + dest.getNodeValue();
				}
			}
			for (Node op : DOMHelper.getElementsByTagNs(bindings, "operation")) {
				String name = DOMHelper.getAttrib(op, "name");
				String soapAction = DOMHelper.getNodeValueNs(op, "soapAction");
				operations.put(name, soapAction);				
			}
		}
	}

	public String getTimeout() {
		return timeout;
	}

	public void setTimeout(String timeout) {
		this.timeout = timeout;
	}

	@Override
	public List<String> getDestinations() {
		return Collections.singletonList("soap "+url);		
	}

	@Override
	public void addDestinations(Collection<String> destList) {}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	public void setName(String epName) {
		this.name = epName;		
	}

	public void setUrl(String url) {
		this.url = url;		
	}	

	public void addOperation(String op, String soapAction) {
		operations.put(op, soapAction);		
	}	

	@Override
	public String getGroupingKey() {
		return url!=null ? url : "<empty-url>";
	}

	@Override
	public Endpoint deepClone() {
		SOAPEndpoint soap = new SOAPEndpoint();
		soap.name = name;
		soap.url = url;
		soap.timeout = timeout;
		soap.operations.putAll(operations);
		return soap;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SOAPEndpoint [name=").append(name)
				.append(", operations=").append(operations).append(", url=")
				.append(url).append(", timeout=").append(timeout)
				.append(", isStarter=").append(isStarter).append("]");
		return builder.toString();
	}
}
