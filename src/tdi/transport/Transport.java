/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import tdi.core.ConfigParam;
import tdi.core.JobEventStats;
import tdi.core.License;
import tdi.core.Logger;
import tdi.core.PEProc;

@ConfigParam(name="TSI_TRANSPORT", desc ="Java class name implementing transport", value = "tdi.transport.Transport")
public abstract class Transport {
	
	private static long notSent = 0;
	private static long notSentLastTime = 0;
	
	private final static long NOT_SENT_CNT_TRIGGER = 10;
	private final static long NOT_SENT_TIME_TRIGGER = 450000;
	private volatile static Transport t = null;
	
	protected final static AtomicInteger maxMk = new AtomicInteger(6);
	protected final static String EVT_EXT = ".tde";
	protected String remoteAddress = address();
	
	public abstract Transport init();
		
	public final static String getHostname() {
		String hostname = System.getenv("HOSTNAME");
		if (hostname==null) {
			try {
				hostname = InetAddress.getLocalHost().getHostName();
			}
			catch (UnknownHostException e) {}
		}
		return hostname;
	}
	
	protected static Transport createTransportForBootstrap(String address) {
		Transport t = null;
		try {
			if (address.indexOf('\n')>0)
				t = new MultiTransport(address);
			else if (address.startsWith("jdbc:oracle:thin:"))
				t = new OraTransport();
			else if (address.startsWith("cassandra:"))
				t = new CasaTransport();
			else if (address.equals("void"))
				t = new VoidTransport();
			else 
				t = (Transport) Class.forName(PEProc.getStaticProperty("TSI_TRANSPORT", "tdi.transport.HSQLTransport")).newInstance();
			
			Logger.getInstance().debug("Bootstrap transport for address '"+address+"' is "+t);
			return t;
		}
		catch (Exception e) {
			Logger.getInstance().debug("getTransport ERROR",e);
			return new VoidTransport();
		}
	}
	
	public static LinkedList<JobEventStats> fetchEvents() {
		try {
			return getInstance().retrieveEvents();
		}
		catch (Exception e) {
			Logger.getInstance().debug("Cannot retrieve POP events",e);
			return new LinkedList<JobEventStats>();
		}
	}	

	public static void deliverEvents(String type, LinkedList<JobEventStats> events) {
		if (events.isEmpty())
			return;
		AtomicReference<byte[][]> req = new AtomicReference<byte[][]>();
		
		boolean l = License.isValid();
		for (Iterator<JobEventStats> it = events.iterator(); it.hasNext(); ) {
			JobEventStats je = it.next();
			if (!EventType.AUTO.equals(type)) {
				if (l)
					je.setType(type);
				else
					it.remove();
			}
		}	
		
		if (!getInstance().send(req, events)) {
			notSent++;
			notSentLastTime = System.currentTimeMillis();
			FileOutputStream fos = null;
			try {
				File outFile = new File("./"+System.currentTimeMillis()+EVT_EXT);
				if (!isFileWritable(outFile.getAbsolutePath())) {
					outFile = File.createTempFile("tdi_event", outFile.getName());
					if (!isFileWritable(outFile.getAbsolutePath()))
						outFile = new File("/tmp/"+outFile.getName());
				}
				Long usableSpace = null;
				try {
					usableSpace = outFile.getParentFile().getUsableSpace();
				}
				catch (Throwable t) {}
				if (usableSpace!=null && usableSpace < 512*1024*1024)
					throw new RuntimeException("Not enough disk space");
				fos = new FileOutputStream(outFile);
				fos.write(req.get()[0]);
				fos.write(req.get()[1]);
				fos.close();
			}
			catch (Exception e) {
				Logger.getInstance().debug("serializeToDisk ERROR",e);
				Logger.getInstance().debug("[ERROR] Loosing events "+type+" due to transport error");
			}
		}
		else {
			Transport.resend(false);
		}			
	}

	protected static boolean isFileWritable(String path) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(path);
			fos.write(new java.util.Date().toString().getBytes());
			return true;
		}
		catch (Exception e) {
			return false;
		}
		finally {
			if (fos!=null) {
				try {
					fos.close();
				}
				catch (Exception exc) {}
			}
		}
	}
	
	public static void resend(boolean lookup) {
		if (lookup || notSent>NOT_SENT_CNT_TRIGGER || (notSentLastTime!=0 && System.currentTimeMillis()-notSentLastTime > NOT_SENT_TIME_TRIGGER)) {
			File[] tdis = new File("./").listFiles(new FilenameFilter() {				
				public boolean accept(File dir, String name) {
					return name.endsWith(EVT_EXT);
				}
			});
			if (tdis!=null) {
				int cnt=0;
				notSent = tdis.length;
				for (File f : tdis) {
					if (f.length()==0 || !f.canRead()) {
						notSent--;
						continue;
					}
					FileInputStream fis = null;
					try {
						fis = new FileInputStream(f);
						byte[] buff = new byte[(int) f.length()];
						int n = fis.read(buff);
						try {
							fis.close();
							fis = null;
						}
						catch (Exception ce) {};
						
						if (n==buff.length) {
							if (getInstance().send(buff)) {
								if (!f.delete())
									Logger.getInstance().debug("[ERROR] TDI cannot delete itermediate file "+f.getName());
								notSent--;
							}
							else
								break;
						}
						else
							Logger.getInstance().debug("[ERROR] TDI truncated read: "+f.getPath());
					}
					catch (Exception ioe) {}
					finally {
						if (fis!=null) {
							try {
								fis.close();
							}
							catch (Exception closeExc) {}
						}
					}
					if (++cnt > NOT_SENT_CNT_TRIGGER)
						break;
				}				
			}
			if (notSent==0)
				notSentLastTime = 0;
		}		
	}
	
	public void serializeNotSentEvents(AtomicReference<byte[][]> req, LinkedList<JobEventStats> events) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(events);
			req.set(new byte[][] { baos.toByteArray(), new byte[] {} });				
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			if (oos!=null) {
				try {
					oos.close();
				}
				catch (Exception exc) {}
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "static-access" })
	public LinkedList<JobEventStats> deserializeNotSentEvents(byte[] data) throws Exception {
		LinkedList<JobEventStats> list = null;
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(bais);
			list = (LinkedList<JobEventStats>) ois.readObject();	
			boolean warnSerialization = false;
			for (JobEventStats je : list) {
				if (je.serialVersionUID != JobEventStats.serialVersionUID)
					warnSerialization = true;
			}
			if (warnSerialization)
				Logger.getInstance().debug("WARNING: recovered events have unexpected format");
			return list;
		}
		finally {
			if (ois!=null) {
				try {
					ois.close();
				}
				catch (Exception exc) {}
			}
		}
	}	
	
	public boolean send(AtomicReference<byte[][]> req, LinkedList<JobEventStats> events) {
		try {
			sendEvents(events);			
			return true;
		}
		catch (Exception ex) {
			Logger.getInstance().debug("send ERROR",ex);
			serializeNotSentEvents(req, events);
			return false;
		}		
	}

	public boolean send(byte[] data) {
		LinkedList<JobEventStats> list = null;
		try {
			list = deserializeNotSentEvents(data);
			sendEvents(list);			
			return true;
		}
		catch (Exception ex) {
			Logger.getInstance().debug("send ERROR",ex);
			return false;
		}		
	}
	
	protected abstract void sendEvents(LinkedList<JobEventStats> events) throws Exception;
	
	protected abstract LinkedList<JobEventStats> retrieveEvents() throws Exception;
	
	protected void loadAutoConfig() {}
	
	public static void loadAutoCfg() {
		getInstance().loadAutoConfig();
	}	
	
	protected static String address() {
		return PEProc.getStaticProperty("TSI_ADDRESS", "auto:8192");		
	}

	public static String[] parseHostPort(String remote, int p) {
		if (remote==null || remote.trim().length()==0)
			return new String[] { "localhost", p+"" };
		if (remote.indexOf(":")==-1)
			remote += ":"+p;
		String[] hp = remote.split("\\:");
		try {
			Integer.valueOf(hp[1]);
		}
		catch (Exception e) {
			hp[1] = p+"";
		}
		return hp;		
	}

	public abstract void uploadProcessesIfNeeded(HashMap<String, String[]> retrieveProcessFiles) throws Exception;
	
	public static boolean uploadProcesses(HashMap<String, String[]> data) {
		try {
			getInstance().uploadProcessesIfNeeded(data);
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}
	
	private static synchronized Transport getInstance() {
		if (t == null) {
			String a = address();
			t = createTransportForBootstrap(a).init();						
		}
		return t;
	}
	
	public static synchronized void noTransport() {
		t = new VoidTransport();
	}
}
