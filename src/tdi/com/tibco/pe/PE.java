/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.com.tibco.pe;

import java.sql.Connection;

import tdi.core.ProtectingClassLoader;

/*** ENTRY POINT ***/
public class PE extends com.tibco.pe.PEMain {
	
	public static void main(String args[]) {
        isMain = true;
        try {    
        	ProtectingClassLoader.getInstance()
        	.loadClass("tdi.core.PEProc")
        	.getDeclaredMethod("main", String[].class)
        	.invoke(null, (Object)args);
		}
        catch (Throwable t) {
			t.printStackTrace();
		}
    }
	
	public static void instrument(Connection conn) {
        try {    
        	Class<?> apiClass = ProtectingClassLoader.getInstance()
        	.loadClass("tdi.api");
        	apiClass.getDeclaredMethod("instrument", Connection.class)
        	.invoke(apiClass.newInstance(), conn);
		}
        catch (Throwable t) {
			t.printStackTrace();
		}
    }
}
