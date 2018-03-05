/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.hsqldb.server.Server;

import tdi.core.ConfigParam;
import tdi.core.JVMHelper;
import tdi.core.Logger;
import tdi.core.ProtectingClassLoader;

public class HSQLServer extends Thread {
	
	private final static AtomicReference<HashMap<String,String>> propsRef = new AtomicReference<HashMap<String,String>>();
	private final static AtomicReference<String> address = new AtomicReference<String>("localhost:8192");
	
	public static synchronized HashMap<String, String> loadPropertiesFromFile() {
		HashMap<String, String> propsFromFile = propsRef.get();
		if (propsFromFile==null) {
			propsFromFile = new HashMap<String, String>();
			propsRef.set(propsFromFile);
			File f = new File(System.getProperty("TDI_CONFIG_FILE", "./config.env"));
			if (!f.exists())
				f = new File("./config.env");
			Logger.getInstance().debug("TDI DB Server looking for config file in "+f.getAbsolutePath());
			if (f.exists() && f.canRead()) {
				FileReader fr = null;
				try {
					fr = new FileReader(f);
					BufferedReader br = new BufferedReader(fr);
					String line = null;
					while ((line=br.readLine())!=null) {
						String[] kv = line.split("=");
						if (kv!=null && kv.length==2) {
							propsFromFile.put(kv[0], kv[1]);
						}
					}
					br.close();
					fr.close();
				}
				catch (Exception e) {
					if (fr!=null) {
						try {
							fr.close();
						}
						catch (Exception exc) {}
					}
				}				
			}
		}
		return propsFromFile;
	}
	
	public static String getProperty(String p, String def) {
		String val = loadPropertiesFromFile().get(p);
		if (val!=null)
			return val;
		val = System.getProperty(p);
		if (val!=null)
			return val;
		val = System.getenv(p);
		if (val!=null)
			return val;
		return def;
	}
	
	private final static String DBVER = "v1g"; /*for db tables compatibility*/
	private static Server server = null;
	private static boolean started = false;
	
	private final static String addrInitializer() {
		String host = address.get().split("\\:")[0];
		if (host.equals("localhost") || host.equals("127.0.0.1"))
			host = Transport.getHostname();
		return host+":"+getAvailableServerPort();		
	}
	
	private final static int getAvailableServerPort() {
		int p = 8192;
		try {
			p = Integer.parseInt(address.get().split("\\:")[1]);
		}
		catch (Exception e) {}
		ServerSocket testPortSocket = null;
		try {
			testPortSocket = new ServerSocket(p);
			testPortSocket.setReuseAddress(true);
			testPortSocket.close();
		}
		catch (Exception e) {
			try {
				testPortSocket = new ServerSocket(0);
				testPortSocket.setReuseAddress(true);
				p = testPortSocket.getLocalPort();
				testPortSocket.close();
			}
			catch (Exception ex) {}
		}
		return p;
	}
	
	private final static String[] DB_SCRIPTS = {
		"SET DATABASE SQL SYNTAX ORA TRUE",
		"SET DATABASE DEFAULT TABLE TYPE CACHED",
		"SET DATABASE EVENT LOG LEVEL 1",
		"SET FILES WRITE DELAY 5000 MILLIS",
		
		/*"SET DATABASE SQL LONGVAR IS LOB FALSE",*/
		"SET TABLE SYSTEM_LOBS.BLOCKS TYPE CACHED",
		"SET TABLE SYSTEM_LOBS.LOBS TYPE CACHED",
		"SET TABLE SYSTEM_LOBS.LOB_IDS TYPE CACHED",
		
		"SET DATABASE DEFAULT RESULT MEMORY ROWS 2048",
		"SET FILES LOB COMPRESSED TRUE",
		"SET DATABASE TRANSACTION ROLLBACK ON CONFLICT TRUE",
		"SET DATABASE TRANSACTION CONTROL MVCC",
		
		"create table if not exists dual(dummy varchar(1));",
		
		"create table if not exists EVENTS_T(id identity primary key, version bigint, "+
		"host varchar2(200), repo varchar2(200), proc varchar2(2000), job bigint,"+
		"cr numeric, fn numeric, rn numeric, fl numeric, tc numeric, st bigint," +
		"d bigint, en bigint, wt bigint, et bigint, ec bigint, type varchar2(200), jts bigint, jexp bigint, jdc bigint, " +
		"metrics longvarchar, em longvarchar," +
		"markers_defs longvarchar, markers longvarchar, markers_msgs longvarchar," +
		"mk1 varchar2(2000), mk2 varchar2(2000), mk3 varchar2(2000)," +
		"mk4 varchar2(2000), mk5 varchar2(2000), mk6 varchar2(2000));",
		
		"create table if not exists EVENTS_DATA(id identity primary key, host varchar2(200), repo varchar2(200), proc varchar2(2000), "+
		"job bigint, version bigint, "+
		"kind varchar2(4000), data varchar2(4000), input longvarchar, output longvarchar, "+
		"v1 bigint, v2 bigint, v3 bigint, "+
		"v4 bigint, v5 bigint, v6 bigint);",
		
		/*"create index IDX_ET_HOST on EVENTS_T(host);",*/	
		"create index IDX_ET_HOST_REPO on EVENTS_T(host,repo);",
		"create index IDX_ET_REPO_PROC on EVENTS_T(repo,proc);",
		"create index IDX_ET_TYPE on EVENTS_T(type);",
		"create index IDX_ET_JOB on EVENTS_T(job);",
		"create index IDX_ED_HOST_REPO on EVENTS_DATA(host,repo);",
		"create index IDX_ED_REPO_PROC on EVENTS_DATA(repo,proc);",
		"create index IDX_ED_JOB on EVENTS_DATA(JOB);",
		
		"create index IDX_MK1 on EVENTS_T(mk1);",
		"create index IDX_MK2 on EVENTS_T(mk2);",
		"create index IDX_MK3 on EVENTS_T(mk3);",
		"create index IDX_MK4 on EVENTS_T(mk4);",
		"create index IDX_MK5 on EVENTS_T(mk5);",
		"create index IDX_MK6 on EVENTS_T(mk6);",
		
		"CREATE FUNCTION ms_to_ts(x BIGINT) RETURNS TIMESTAMP NO SQL "+
		"LANGUAGE JAVA PARAMETER STYLE JAVA EXTERNAL NAME "+
		"'CLASSPATH:tdi.transport.HSQLServer.msToTs'",
		
		"create table if not exists USAGE_T(id identity primary key, version bigint, "+
		"host varchar2(200), repo varchar2(200), cpu real, mem real, gc real, "+
		"st bigint);",		
		
		"create table if not exists PARAMS_T(id identity primary key, version bigint, "+
		"host varchar2(200), repo varchar2(200), param varchar2(200), value varchar2(4000));",
		
		"create index IDX_PT_HOST_REPO on PARAMS_T(host,repo);",
		
		"create table if not exists POP_T(id identity primary key, version bigint, "+
		"host varchar(200), repo varchar2(200), type varchar2(200), key varchar2(4000), value clob);",
				
		"create index IDX_POP_HOST_REPO on POP_T(host,repo);",		
	};
		
	public final static java.sql.Timestamp msToTs(long x) {
		return new java.sql.Timestamp(x);
	}
	
	public final static String init(String requestedAddress) {
		if (started)
			return address.get();
		address.set(requestedAddress);
		address.set(addrInitializer());
		try {
			/* server already running? */
			new tdi.sql.JDBCDriver();
			DriverManager.getConnection("tdi:"+address.get(), "sa", "").close();
			started = true;
			return address.get();
		}
		catch (Exception exc) {}
		try {
			server = createServer();
			server.setDaemon(false);
			Runtime.getRuntime().addShutdownHook((Thread)JVMHelper.addShutdownHook(new Thread() {
				public void run() {
					HSQLDiscovery.stopAdvertizer();
					try {
						server.stop();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					try {
						server.shutdown();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}));
			server.start();
			try {
				HSQLDiscovery.startAdvertizer(address.get());
			}
			catch (Exception exc) {
				Logger.getInstance().debug("Cannot init advertizer", exc);
			}
			Connection conn = DriverManager.getConnection("tdi:"+address.get(), "sa", "");
			Statement st = null;
			for (String sql : DB_SCRIPTS) {
				try {
					st = conn.createStatement();
					st.execute(sql);
				}
				catch (Exception ste) {
					String msg = ste.getMessage();
					if (msg==null || !msg.contains("already exists")) {
						Logger.getInstance().debug("dbScript('"+sql+"') ERROR",ste);
					}
				}
				finally {
					try {
						if (st!=null)
							st.close();				
					}
					catch (Exception esc) {}
				}
			}
			try {
				if (conn!=null)
					conn.close();
			}
			catch (Exception ecc) {}
			started = true;
			Logger.getInstance().debug("TDI HSQLServer started on "+address.get()+" in "+server.getDatabasePath(0, true));
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return address.get();
	}
	
	private static Server createServer() {
		String[] hp = address.get().split(":");
		int port = Integer.valueOf(hp[1]);
		try {
			Server srv = new Server() {
				public int start() {
					maxConnections = 4096;
					return super.start();
				}
			};
			srv.setDatabaseName(0, "tdi");
			srv.setDatabasePath(0, accessibleDbPath("./tdidb."+DBVER+"/store"));
			srv.setAddress("0.0.0.0");
			srv.setPort(port);
			srv.setSilent(true);
			return srv;
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private static String accessibleDbPath(String path) {
		File f = new File(path+"/testfile");
		String pth = f.getParentFile().getAbsolutePath();
		if (Transport.isFileWritable(pth))
			return pth;
		
		File testFile = null;
		try {
			testFile = File.createTempFile(path.replace('.', '_').replace('/', '_'), "x");
			pth = testFile.getParentFile().getAbsolutePath();
			if (Transport.isFileWritable(pth))
				return pth;
		}
		catch (Exception exc) {}
		pth = "/tmp/"+path;
		if (Transport.isFileWritable(pth))
			return pth;
		
		return new File(path).getAbsolutePath();
	}

	@ConfigParam(name="TSI_ADDRESS", desc="Address for TDI native database server", value="host:port")
	public final static void main(String[] args) {
		Logger.getInstance().debug("Starting HSQLServer");		
		HSQLServer.init(System.getProperty("TSI_ADDRESS","localhost:8192"));
	}
	
	@ConfigParam(name="DB_JAVA_OPTS", desc="JVM commandline options for starting DB, -D only", value="-Xmx1g")
	public static String create(String remoteAddress) {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			pb.command("java", System.getProperty("DB_JAVA_OPTS", "-DO_SET_MY_MEM=please"),
				"-DTSI_ADDRESS="+remoteAddress, "-cp", ProtectingClassLoader.getJarName(),
				HSQLServerLoader.class.getName());
			Logger.getInstance().debug("Requested DB start: "+pb.command());
			Process p = pb.start();
			try {
				p.exitValue();
				return null;
			}
			catch (Exception e) {}
			return remoteAddress;
		}
		catch (Exception exc) {
			exc.printStackTrace();
		}
		return null;
	}
}
