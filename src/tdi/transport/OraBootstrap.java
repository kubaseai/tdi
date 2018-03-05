/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;

import tdi.core.Logger;

public class OraBootstrap {

	public static void setup(Connection conn) throws Exception {
		InputStream is = OraBootstrap.class.getResourceAsStream("/bootstrap.sql");
		String sqlCommand = null;
		try {
			if (is!=null) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] b = new byte[4096];
				while (true) {
					int cnt = is.read(b);
					if (cnt >= 0)
						baos.write(b, 0, cnt);
					else
						break;
				}
				baos.flush();
				byte[] buff = baos.toByteArray();
				baos.close();
				Statement st = null;
				Logger.getInstance().debug("Oracle bootstrap started");
				for (String sql : new String(buff).split("\\;;")) {
					sql = sqlCommand = escapeSql(sql);
					if (sql.length()>0) {
						if (st==null)
							st = conn.createStatement();
						st.executeUpdate(sql);
					}
				}
				if (st!=null) {
					try {
						st.close();
					}
					catch (Exception ex) {}
				}
			}	
		}
		catch (Exception ex) {
			Logger.getInstance().debug("Oracle bootstrap failed on "+sqlCommand);
			throw ex;
		}
		finally {
			if (is!=null) {
				try {
					is.close();
				}
				catch (Exception ex) {}
			}
		}
	}

	private static String escapeSql(String sql) {
		StringBuffer sb = new StringBuffer();
		boolean afterWh = false;
		for (int i=0; i < sql.length(); i++) {
			char c = sql.charAt(i);
			if (c != '\r' && c!= '\n' && c != '\t') {
				if (c!=' ') {
					sb.append(c);
					afterWh = true;
				}
				else if (afterWh)
					sb.append(c);
			}
		}
		if (sb.indexOf("begin")==0)
			return sb.toString().trim()+";";
		else
			return sb.toString();
	}	
}
