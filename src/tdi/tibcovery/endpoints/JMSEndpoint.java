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
import org.w3c.dom.Node;

import tdi.tibcovery.common.DOMHelper;

public class JMSEndpoint extends Endpoint {
	
	protected JMSEndpoint() {}
	
	String jmsConnRef;
	String transacted;
	String maxSessions;
	String destination;
	String selector;
	
	public JMSEndpoint(Node _cfg, Node input) {
		jmsConnRef = DOMHelper.getNodeValueNs(_cfg, "ConnectionReference");
		Node sessAttr = DOMHelper.getElementByTagNs(_cfg, "SessionAttributes", 0);
		transacted = DOMHelper.getNodeValueNs(sessAttr, "transacted");
		maxSessions = DOMHelper.getNodeValueNs(sessAttr, "maxSessions");
		destination = DOMHelper.getNodeValueNs(sessAttr, "destination");
		selector = DOMHelper.getNodeValueNs(sessAttr, "selector");
		Node activityInput = DOMHelper.getElementByTagNs(input, "ActivityInput", 0);
		String dest = DOMHelper.getNodeValueNs(activityInput, "destinationQueue");
		if (dest!=null) {
			destination = dest;			
		}
	}

	@Override
	public Collection<String> getDestinations() {
		return Collections.singletonList("jms "+destination);		
	}

	@Override
	public Endpoint deepClone() {
		JMSEndpoint ep = new JMSEndpoint();
		ep.jmsConnRef = jmsConnRef;
		ep.transacted = transacted;
		ep.maxSessions = maxSessions;
		ep.destination = destination;
		ep.selector = selector;
		return ep;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JMSEndpoint [jmsConnRef=").append(jmsConnRef)
				.append(", transacted=").append(transacted)
				.append(", maxSessions=").append(maxSessions)
				.append(", destination=").append(destination)
				.append(", selector=").append(selector)
				.append(", isStarter=").append(isStarter)
				.append("]");
		return builder.toString();
	}
}
