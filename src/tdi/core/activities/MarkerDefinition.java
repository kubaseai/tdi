/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.core.activities;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tibco.xml.datamodel.XiNode;
import com.tibco.xml.datamodel.helpers.XiSerializer;

import tdi.core.ConfigParam;
import tdi.core.JobEventStats;
import tdi.core.Logger;
import tdi.core.PEProc;
import tdi.core.ValueHolder;

@ConfigParam(name="SKIP_MK_MESSAGE", desc="Skip storing full message when any of its nodes matches some marker", value="true|false")
public class MarkerDefinition {	
	
	private final static ValueHolder<Boolean> skipMessage = 
		PEProc.getVolatileBooleanProperty("SKIP_MK_MESSAGE", "false");
	
	private static class MarkerDefinitionHolder {
		private static MarkerDefinition inst = null;		
		public static synchronized MarkerDefinition getInstance() {
			String currentMarkersRaw = getMarkersRaw();
			if (inst==null || markersRaw==null || !markersRaw.equals(currentMarkersRaw)) {
				markersRaw = currentMarkersRaw;
				inst = MarkerDefinition.load();
			}
			return inst;
		}
	}	
		
	@ConfigParam(name="MARKERS", desc="follow schema MARKER_$Name:$Value, where $Name can be string for non-indexed marker and number 1-4 for indexed marker. "+
			"$Value is simple XPath expresion for selecting XML nodes, in which values devops are interested to. Example: MARKER_1://orders/orderId. When data matches "+
			"marker expression the exact node value is stored as a marker value and also whole message is store in separate object. Try MARKER_ALL:*", value="string")

	protected String markerName = "";
	protected String path = "";
	private static String markersRaw = getMarkersRaw();
	private LinkedList<XPathMarkerDefinition> list = new LinkedList<XPathMarkerDefinition>();
	
	private final static MarkerDefinition load() {
		MarkerDefinition md = new MarkerDefinition();		
		if (markersRaw!=null && markersRaw.length()>0) {
			String[] defs = markersRaw.split("MARKER_");
			for (String s : defs) {
				if (s!=null && s.length()>0) {
					int i = s.indexOf(':');
					if (i!=-1) {
						String name = s.substring(0, i).trim();
						String elementsPath = s.substring(i+1).trim();
						md.list.add(new XPathMarkerDefinition(name, elementsPath));						
					}
					else
						Logger.getInstance().debug("Invalid marker: '"+s+"'");					
				}
			}
		}		
		return md;
	}	
	
	private static String getMarkersRaw() {
		return PEProc.getRuntimeProperty("MARKERS", "");		
	}

	public static String getRawString() {
		return markersRaw;
	}
	
	protected void print(StringBuffer all, StringBuffer sb, MarkerDefinition md) {		
		if (list.size()==0) {
			all.append(markerName).append("=").append(path);
		}
		else {
			all.append(list);
		}				
	}
	
	public String toString() {
		StringBuffer all = new StringBuffer("MarkerDefinition[");
		print(all, null, this);
		all.append("]");
		return all.toString();
	}
	
	protected void debugPrint() {
		Logger.getInstance().debug(toString());
	}	
	
	public static void processXiNode(AtomicBoolean includeMessage, JobEventStats jl, XiNode root, List<XPathMarkerDefinition> list) {
		for (XPathMarkerDefinition cmd : list) {
			if (cmd.path.length()>0 && cmd.path.replace("/", "").replace("*","").length()==0) {
				includeMessage.set(true);
				return;
			}
			if (cmd.matches(root)) {
				for (String nodeValue : cmd.getMatches()) {				
					jl.putMarker(cmd.markerName, nodeValue);
					try {
						char c = cmd.markerName.charAt(0);
						if (c >= '0' && c <= '9') {
							int mk = Integer.valueOf(cmd.markerName);
							jl.addGlobalMarker(mk, nodeValue);
						}
					}
					catch (Exception e) {}
					includeMessage.set(true);
				}
				return;
			}
		}		
	}	

	public static void processNode(JobEventStats jl, XiNode input, int kind) {
		if (MarkerDefinitionHolder.getInstance().list.isEmpty() || (input==null || !input.hasChildNodes()))
			return;
		AtomicBoolean includeMessage = new AtomicBoolean(false);
		processXiNode(includeMessage, jl, input, MarkerDefinitionHolder.getInstance().list);
		if (includeMessage.get() && !skipMessage.get()) {
			String hint = jl.getActivityNameAsXmlComment();
			String msg = XiSerializer.serialize(input);
			if (jl.putMarkerMessage(hint, msg)) {
				jl.setLastMarkerMessage(msg, kind);
			}
		}		
	}
	
	public static void processInput(JobEventStats jl, XiNode input) {		
		processNode(jl, input, 0);
	}

	public static void processData(JobEventStats jl, XiNode data) {		
		processNode(jl, data, 1);
	}
}
