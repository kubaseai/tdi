/*
 * Copyright (c) 2010-2011 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import tdi.core.Logger;

public class HTTPServer extends Thread {
	
	private static boolean started = false;
	private int port = 0;
	
	public HTTPServer(int port) {
		this.port = port;
		this.setName("HTTPServer #"+hashCode());
	}
	
	public void run() {
		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
      	                Executors.newCachedThreadPool()));
		bootstrap.setPipelineFactory(new HTTPPipelineFactory());
		bootstrap.bind(new InetSocketAddress(port));		
	}
	
	public static void init() {
		if (started)
			return;
		String[] hp = Transport.address().split("\\:");
		new HTTPServer(Integer.valueOf(hp[1])).start();
		started = true;
		Logger.getInstance().debug("TDI HTTPServer started on "+Transport.address());				
	}
}
