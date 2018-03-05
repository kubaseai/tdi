/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.test;

import java.net.URL;
import java.net.URLClassLoader;

public class CL {
	
	public final static void main(String[] args) throws Exception {
		URL u = new URL("jar:file:C:\\tibco\\bw\\5.9\\hotfix\\lib\\ojdbc.tdi!/");
		URLClassLoader cl = new URLClassLoader(new URL[] { u }, Thread.currentThread().getContextClassLoader());
		cl.loadClass("oracle.jdbc.OracleDriver").newInstance();	
		cl.close();
	}
}
