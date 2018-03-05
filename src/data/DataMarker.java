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

package data;

import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DataMarker {
	
	public static class JarContent {
		public LinkedList<InputStream> entries;
		public LinkedList<String> names;
	}
	
	public final static JarContent getXmlData() {
		LinkedList<InputStream> entries = new LinkedList<InputStream>();
		LinkedList<String> names = new LinkedList<String>();
		URL start = null;
		try {
			start = DataMarker.class.getResource("DataMarker.class");
		}
		catch (Exception e) {}
		if (start!=null) {
			try {
				URLConnection conn = start.openConnection();
				if (conn instanceof JarURLConnection) {
					JarURLConnection jconn = (JarURLConnection) conn;
					JarFile jf = jconn.getJarFile();
					Enumeration<JarEntry> en = jf.entries();
					while (en.hasMoreElements()) {
						JarEntry je = en.nextElement();
						if (je.isDirectory() || !je.getName().startsWith("data/"))
							continue;
						String nm = je.getName();
						if (nm.endsWith(".xml")) {
							entries.add(jf.getInputStream(je));
							names.add(nm.substring(nm.lastIndexOf("/")));
						}
					}
				}
			}
			catch (Exception e) {
				System.err.println("Cannot process jar file: "+e.getMessage());
			}
		}
		JarContent jc = new JarContent();
		jc.entries = entries;
		jc.names = names;
		return jc;		
	}
}
