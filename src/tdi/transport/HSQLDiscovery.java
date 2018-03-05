package tdi.transport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import tdi.core.Logger;

public class HSQLDiscovery {
	
	private static boolean advertizerCanRun = true;
	private static Thread advertizer = null;
	private static Thread listener = null;
	private final static String PAYLOAD_START = "{\"tdi.addr\": \"";

	public static String findActiveNode(Integer udpPort, final int timeoutMillis, AtomicBoolean received) {
		if (received!=null)
			received.set(false);
		DatagramSocket sock = null;
		final AtomicReference<DatagramSocket> socketRef = new AtomicReference<DatagramSocket>();
		try {
			sock = new DatagramSocket(null);
			sock.setSoTimeout(timeoutMillis);
			sock.setReuseAddress(true);
			sock.bind(new InetSocketAddress(udpPort));
			socketRef.set(sock);
		}
		catch (Exception e) {
			try {
				if (sock!=null)
					sock.close();
			}
			catch (Exception exc) {}
			return "127.0.0.1:"+udpPort;
		}
		
		final LinkedBlockingQueue<String> resultQueue = new LinkedBlockingQueue<String>();
		listener = new Thread() {
			public void run() {
				long endTime = System.currentTimeMillis() + timeoutMillis;
				DatagramPacket p = new DatagramPacket(new byte[4096], 4096);
				while (System.currentTimeMillis() < endTime) {
					try {
						socketRef.get().receive(p);
						if (p.getLength() > 0) {
							String payload = new String(p.getData());
							if (payload.startsWith(PAYLOAD_START)) {
								payload = payload.substring(PAYLOAD_START.length(), payload.lastIndexOf("\"}"));
								resultQueue.add(payload);
							}
						}
					}
					catch (IOException e) {}
				}
				try {
					socketRef.get().close();
				}
				catch (Exception e) {}
			}
		};
		listener.start();
		String result = null;
		try {
			result = resultQueue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
			if (result!=null)
				received.set(true);
		}
		catch (Exception e) {}
		return result != null ? result : ("localhost:"+udpPort);
	}

	public static void stopAdvertizer() {
		advertizerCanRun = false;		
	}

	private static DatagramPacket createAdvertiserMessage(String addr, int port) throws SocketException {
		byte[] msg = (PAYLOAD_START+addr+"\"}").getBytes();
		InetSocketAddress target = new InetSocketAddress("255.255.255.255", port);
		return new DatagramPacket(msg, msg.length, target);
	}
	
	public static void startAdvertizer(final String addr) throws Exception {
		Integer port = null;
		String[] hp = addr.split(":");
		try {
			port = (Integer.valueOf(hp[1]));
		}
		catch (Exception e) {
			throw new Exception("Invalid port given for advertizer: "+addr);
		}
		DatagramSocket sock = null;
		final AtomicReference<DatagramSocket> socketRef = new AtomicReference<DatagramSocket>();
		try {
			sock = new DatagramSocket();
			sock.setBroadcast(true);
			socketRef.set(sock);			
		}
		catch (Exception e) {
			try {
				if (sock!=null)
					sock.close();
			}
			catch (Exception exc) {}
			throw e;
		}
		final DatagramPacket p = createAdvertiserMessage(addr, port);
		advertizer = new Thread() {			
			public void run() {		
				Logger.getInstance().debug("Starting advertiser for "+addr);				
				while (advertizerCanRun) {
					try {
						socketRef.get().send(p);
					}
					catch (IOException e) { /* this is UDP, ignore */}
					try {
						Thread.sleep(1000);
					}
					catch (InterruptedException e) {}
				}
				try {
					socketRef.get().close();
				}
				catch (Exception e) {}
			}
		};
		advertizer.start();
	}
}
