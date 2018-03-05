package com.tibco.pe.core;

import java.io.Serializable;

/**
 * This is a magic to automatically load TDI without any reconfiguration of BW components
 */
public class StarterEvent implements Serializable {

	public static final long serialVersionUID = 0x7D17D1;
	
	static {
		tdi.api.INSTANCE().instrument(null);
	}
		
	StarterEvent(JobCreator jobcreator) {
		name = jobcreator.getName();
		starterName = jobcreator.getStarterActivityName();
		state = jobcreator.getStateAsString();
	}

	public String getName() {
		return name;
	}

	public String getStarterName() {
		return starterName;
	}

	public String getState() {
		return state;
	}

	String name;
	String starterName;
	String state;
}