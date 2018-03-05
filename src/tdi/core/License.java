/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.HashMap;

public class License implements Serializable {
	
	private static final long serialVersionUID = 5708734203229994714L;
	private static boolean initialized = false;
	private static long result = -1;
	
	public static boolean isValid() {
		if (!initialized)
			init();
		return result == 1L;
	}

	private static void init() {
		InputStream is = License.class.getResourceAsStream("/license.txt");
		if (is!=null) {
			try {
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
				HashMap<String,String> map = new HashMap<String, String>();
				for (String s : new String(buff).split("\\;")) {
					String[] kv = s.trim().split("\\:");
					if (kv.length==2) {						
						map.put(kv[0].replace("Granted ", "").trim(), kv[1].trim());
					}
				}
				result = calculateSerial(map);
				String host = tdi.transport.Transport.getHostname();
				String machines = map.get("machines");
				if (!(machines+"").toLowerCase().contains(host.toLowerCase()) &&
						!"*".equals(machines))
					result = 0;
				String till = map.get("till");
				if (till!=null && till.length()==10) {
					try {
						String[] v = till.split("\\-");
						if (v.length==3) {
							Calendar c = Calendar.getInstance();
							for (int i=0; i < 3; i++)
								c.set(2*i + (i+1)%2, Integer.valueOf(v[i]) - (3-(i+1)) % 3 % 2);
							if (Calendar.getInstance().compareTo(c)>-1)
								result = 0;
						}
					}
					catch (Exception exc) {
						result = 0;
					}
				}
				else
					result = 0;				
			}
			catch (Exception ex) {}
			finally {
				try {
					is.close();
				}
				catch (Exception e) {}
			}			
		}
		if (result!=1) {
			Logger.getInstance().debug("You are using unlicensed version!");			
		}
	}

	private static long calculateSerial(HashMap<String, String> map) throws NoSuchAlgorithmException {
		String to = map.get("to");
		String till = map.get("till");
		String machines = map.get("machines");
		String strs = to+till+machines;
		
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] res = md.digest(strs.getBytes());
		StringBuffer sb = new StringBuffer();
		for (int i=0; i < res.length; i++) {
			sb.append(Integer.toHexString(0xFF & res[i]));			
		}
		
		String validSerial = sb.toString().substring(24).toUpperCase();
//		if (JobEventStats.getRepositoryName().length()==0)
//			System.out.println("Comparing serial "+validSerial+" to "+map.get("Serial"));
		try {
			long diff = map.get("Serial")!=null ? 
				Long.decode("0x"+map.get("Serial").toLowerCase()) - Long.decode("0x"+validSerial) : -1L;
			return diff;
		}
		catch (Exception exc) {}
		return 0;
	}
	
	public final static void main(String[] args) {
		System.out.println(License.isValid());
	}
}
