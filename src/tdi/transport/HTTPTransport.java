/*
 * Copyright (c) 2010-2011 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

import tdi.core.JobEventStats;
import tdi.core.Logger;
import tdi.core.PEProc;

public class HTTPTransport extends Transport {
	
	public final static int max_net_wait = Integer.valueOf( PEProc.getStaticProperty("TSI_MAX_NET_WAIT", "3000") );
	private static byte[] respBuff = new byte[512];
	
	public Transport init() {
		if (remoteAddress.startsWith("localhost:"))
			HTTPServer.init();
		return this;		
	}
	
	private final static String post = "POST /TSI/services/receiver HTTP/1.1\r\n"+
			"Content-Type: text/xml;charset=UTF-8\r\n"+
			"Connection: keep-alive\r\n"+
			"SOAPAction: \"\"\r\n"+
			"Host: ";
	
	private final static String cl = "Content-Length: ";
	private final static String req1 = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
			"xmlns:t=\"http://tsi/\"><s:Header/><s:Body><t:event><t:msg>";

	private final static String req2 = "</t:msg></t:event></s:Body></s:Envelope>";
	
	private static Socket s = null;
	
	private byte[][] makeRequest(LinkedList<JobEventStats> events) {
		StringBuffer sb = new StringBuffer(post);
		sb.append(remoteAddress);		
		sb.append("\r\n");
		sb.append(cl);
		
		StringBuffer r = new StringBuffer();
		r.append(req1);
		for (JobEventStats je : events) {
			r.append(je.toString()).append(" ");
		}
		r.deleteCharAt(r.length()-1);
		r.append(req2);
		
		byte[] rb = r.toString().getBytes();
		sb.append(rb.length);
		sb.append("\r\n\r\n");
		
		byte[] hb = sb.toString().getBytes();
		return new byte[][] { hb, rb };
	}

	private boolean initSocket() {
		if (s==null) {
			try {
				s = new Socket();
				s.setKeepAlive(true);
				s.setReceiveBufferSize(4096);
				s.setSendBufferSize(65536);
				s.setSoLinger(true, max_net_wait);
				s.setReuseAddress(true);
				s.setTcpNoDelay(true);
				s.setSoTimeout(max_net_wait);				
			}
			catch (Exception e) {
				Logger.getInstance().debug("[ERROR] TDI cannot set socket options");
			}			
		}
		if (!s.isConnected() || s.isClosed()) {
			String[] hp = remoteAddress.split("\\:");
			try {
				s.connect(new InetSocketAddress(hp[0], Integer.valueOf(hp[1])), 3000);				
			}
			catch (Exception ex) {
				Logger.getInstance().debug("[ERROR] TDI cannot connect to server");
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean send(AtomicReference<byte[][]> req, LinkedList<JobEventStats> events) {
		byte[][] r = makeRequest(events);
		req.set(r);
		if (!initSocket())
			return false;		
		for (int i=0; i < 2; i++) {
			try {
				OutputStream os = s.getOutputStream();
				InputStream is = s.getInputStream();
				os.write(r[0]);
				os.write(r[1]);
				os.flush();
				int n = 0;
				try {
					n = is.read(respBuff);
				}
				catch (SocketTimeoutException ste) {}
				String resp = "no response";
				if (n>0) {
					resp = new String(respBuff, 0, n);
					if (resp.indexOf("Connection: close")>-1) {
						try {
							s.close();							
						}
						catch (Exception ce) {}
						s = null;
					}						
					if (resp.indexOf(">OK<")>-1)
						return true;					
				}	
				Logger.getInstance().debug("[ERROR] TSI invalid response: "+resp);
			}
			catch (Exception e) {
				Logger.getInstance().debug("HTTP transport ERROR",e);
				try {
					s.close();
				}
				catch (Exception ce) {}
				s = null;
				if (!initSocket())
					return false;
			}
		}
		return false;
	}

	@Override
	public boolean send(byte[] data) {
		if (!initSocket())
			return false;
		for (int i=0; i < 2; i++) {
			try {
				OutputStream os = s.getOutputStream();
				InputStream is = s.getInputStream();
				os.write(data);
				os.flush();
				int n = is.read(respBuff);
				String resp = "no response";
				if (n>0) {
					resp = new String(respBuff, 0, n);
					return resp.indexOf(">OK<")>-1;
				}	
				Logger.getInstance().debug("[ERROR] TSI invalid response: "+resp);
			}
			catch (Exception e) {
				try {
					s.close();
				}
				catch (Exception ce) {}
				s = null;
				if (!initSocket())
					return false;
			}
		}
		return false;
	}

	@Override
	protected void sendEvents(LinkedList<JobEventStats> events) throws IOException {}

	@Override
	public void uploadProcessesIfNeeded(
			HashMap<String, String[]> retrieveProcessFiles) throws Exception {
		/** FIXME: implement */				
	}
	
	@Override
	protected LinkedList<JobEventStats> retrieveEvents() throws Exception {
		/** FIXME: implement */	
		return new LinkedList<JobEventStats>();
	}
}

