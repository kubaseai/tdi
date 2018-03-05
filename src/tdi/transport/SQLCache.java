/*
 * Copyright (c) 2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import tdi.core.ConfigParam;
import tdi.core.JVMHelper;
import tdi.core.PEProc;
import tdi.core.ValueHolder;

public class SQLCache {
	
	@ConfigParam(name="CONN_POOL_LIFETIME", desc="Keep JDBC connection open for given time in milliseconds", value="0|3600000|long")
	private final static ValueHolder<Long> CONN_POOL_LIFETIME = 
		PEProc.getVolatileLongProperty("CONN_POOL_LIFETIME", "0");
	
	private final static HashMap<Connection, HashMap<String, PreparedStatement>> cps =
		new HashMap<Connection, HashMap<String, PreparedStatement>>();
	private final static ConcurrentHashMap<Connection, Long[]> cusage =
			new ConcurrentHashMap<Connection, Long[]>();
			
	static {
		Runtime.getRuntime().addShutdownHook((Thread)JVMHelper.addShutdownHook(new Thread() {
			public void run() {
				purgeOld(-1);
			}
		}));
	}

	public static void registerNewConnection(Connection conn) {
		long now = System.currentTimeMillis();
		cusage.put(conn, new Long[] { now, now });		
	}

	public static boolean deregisterConnection(Connection conn, boolean forceClose) {
		if (conn==null)
			return false;
		Long[] t = cusage.get(conn);
		if (t==null || t[1]-t[0] > CONN_POOL_LIFETIME.get())
			forceClose = true;
		if (forceClose) {
			HashMap<String, PreparedStatement> ps = cps.get(conn);
			if (ps!=null) {
				for (PreparedStatement prep : ps.values()) {
					try {
						if (prep!=null)
							prep.close();
					}
					catch (Exception exc) {}
				}
				ps.clear();
			}
			cusage.remove(conn);
			cps.remove(conn);
		}
		return forceClose;
	}
	
	private synchronized static HashMap<String, PreparedStatement> getCache(Connection conn) {
		HashMap<String, PreparedStatement> cache = cps.get(conn);
		if (cache == null)
			cps.put(conn, cache = new HashMap<String, PreparedStatement>());
		return cache;
	}

	public static PreparedStatement prepareStatement(Connection conn, String sql) throws SQLException {
		Long[] t = cusage.get(conn);
		if (t!=null)
			t[1] = System.currentTimeMillis();
		HashMap<String,PreparedStatement> cache = getCache(conn);
		PreparedStatement st = cache!=null ? cache.get(sql) : null;
		if (st!=null) {
			st.clearBatch();
			st.clearParameters();
			return st;
		}
		st = conn.prepareStatement(sql/*, new String[]{"ID"}*/);
		cache.put(sql, st);
		return st;
	}

	public static void closeStatement(Connection conn, PreparedStatement st) throws SQLException {
		if (st==null)
			return;
		HashMap<String,PreparedStatement> cache = getCache(conn);
		if (cache.size()==0 || !cache.containsValue(st)) {
			try {
				st.close();
			}
			catch (Exception exc) {}
		}
	}

	private static void purgeOld(long val) {
		LinkedList<Connection> closedList = new LinkedList<Connection>();
		for (Entry<Connection,Long[]> en : cusage.entrySet()) {
			Long[] t = en.getValue();
			if (t==null || t[1]-t[0] > val) {
				closedList.add(en.getKey());
			}
		}
		for (Connection conn : closedList) {
			try {
				deregisterConnection(conn, true);
				conn.close();
			}
			catch (Exception e) {}
		}
	}
	
	public static void purgeOld() {
		purgeOld(CONN_POOL_LIFETIME.get());
	}

	public static void overrideConnection(Connection bwTdiConnection) {
		/** FIXME: implement */
	}
}
