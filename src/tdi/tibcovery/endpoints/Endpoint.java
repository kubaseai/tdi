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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public abstract class Endpoint {
	
	private final static XStream xs = new XStream(new DomDriver());
	
	protected TreeSet<String> destinations = new TreeSet<String>();
	private LinkedList<Endpoint> siblings = new LinkedList<Endpoint>();
	protected Endpoint internalEndpoint = null;
	protected HashMap<String,String> props = new HashMap<String, String>();
	protected boolean isStarter = false;
	
	public Endpoint() {}
	
	public Collection<String> getDestinations() {
		return destinations;
	}
	
	public void addDestinations(Collection<String> destList) {
		for (String d : destList)
			if (d!=null)
				destinations.add(d);
	}
	
	public String getGroupingKey() {
		return this.getClass().getName();
	}

	private static void cut(LinkedList<Endpoint> epList, Endpoint e) {
		Iterator<Endpoint> it = epList.iterator();
		if (epList.size()>0)
			e.addSibling(e.deepClone());
		while (it.hasNext()) {
			Endpoint ep = it.next();
			if (e.getGroupingKey().equals(ep.getGroupingKey()) && ep!=e) {
				e.addDestinations(ep.getDestinations());
				it.remove();
				e.addSibling(ep);
			}
		}
	}
	
	public abstract Endpoint deepClone();

	public void addSibling(Endpoint endp) {
		siblings.add(endp);		
	}
	
	public static void groupInPlace(LinkedList<Endpoint> externalEndpoints) {
		for (int i=0; i < externalEndpoints.size(); i++)
			cut(externalEndpoints, externalEndpoints.get(i));
	}

	public static LinkedList<Endpoint> group(LinkedList<Endpoint> externalEndpoints) {
		LinkedList<Endpoint> eepCopy = new LinkedList<Endpoint>();
		for (Endpoint ep : externalEndpoints) {
			eepCopy.add(ep.deepClone());
		}
		Endpoint.groupInPlace(eepCopy);
		return eepCopy;
	}
		
	public LinkedList<Endpoint> getSiblings() {
		return siblings;
	}

	public Endpoint getSubInternalEndpoint() {
		return internalEndpoint;
	}
	
	public Map<String,String> getProperties() {
		return Collections.unmodifiableMap(props);
	}

	public String description() {
		return toString();
	}

	public void setStarter(boolean isStarter) {
		this.isStarter = isStarter;		
	}

	public String toXml() {
		return xs.toXML(this);
	}
}
