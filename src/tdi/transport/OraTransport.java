/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Properties;

import tdi.core.JobEventStats;
import tdi.core.Logger;
import tdi.core.PEProc;

public class OraTransport extends HSQLTransport {	

	private Method createClob = null;
	private Method clobSetString = null;
	private Method setExecuteBatch = null;
	private final static int CLOB_DURATION = 1;	// session duration - old constant	
	private final static int DEFAULT_BATCH_SIZE = 20;
	private ThreadLocal<Driver> drvRef = new ThreadLocal<Driver>();
		
	@Override
	protected void setClob(PreparedStatement ps, int idx, String val) throws SQLException {
		if (val==null)
			ps.setNull(idx, java.sql.Types.CLOB);
		else {
			if (val.length() < 2000) {
				ps.setString(idx, val);
				return;
			}			
			
			Clob clob = null;
			try {
				clob =(Clob) createClob.invoke(null, ps.getConnection(), false, CLOB_DURATION);
				clobSetString.invoke(clob, 1, val);
			}
			catch (Exception exc) {				
				if (exc.getCause()!=null)
					Logger.getInstance().debug("setClob ERROR",exc.getCause());				
				else
					Logger.getInstance().debug("setClob ERROR",exc);
				throw new SQLException("Cannot create clob: "+exc);
			}			
			ps.setClob(idx, clob);
		}
	}
	
	@Override
	protected void setupBatch(PreparedStatement ps) throws SQLException {
		try {
			setExecuteBatch.invoke(ps, new Object[] { DEFAULT_BATCH_SIZE });
		}
		catch (Exception e) {
			for (Throwable t = e.getCause(); t!=null; t = t.getCause()) {
				if (t!=null && t instanceof SQLException)
					throw (SQLException)t;
			}
			throw new SQLException(e);
		}
	}

	@Override
	protected void addBatch(PreparedStatement ps) throws SQLException {
		ps.executeUpdate();
	}

	@Override
	protected void executeBatch(PreparedStatement ps) throws SQLException {
		;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void installSealedDriver() throws Exception {
		if (drvRef.get()==null) {			
			URL jarUrl = this.getClass().getResource("/"+this.getClass().getName().replace('.', '/')+".class");
			if (jarUrl!=null) {
				String dirName = jarUrl.getPath();
				int i = dirName.lastIndexOf("!");
				if (i>0)
					dirName = dirName.substring(0, i);
				i = dirName.lastIndexOf("\\");
				if (i==-1)
					i = dirName.lastIndexOf("/");
				int j = dirName.startsWith("jar:file:") ? 9 : 
					(dirName.startsWith("file:") ? 5 : 0);
				if (i>0)
					dirName = dirName.substring(j, i);
				LinkedList<URL> cpList = new LinkedList<URL>();
				try {
					cpList.add(new URL("jar:file:"+dirName+"/ojdbc.tdi!/"));
					cpList.add(new URL("jar:file:"+dirName+"/orai18n.tdi!/"));
				}
				catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}
				URLClassLoader parent = (URLClassLoader) this.getClass().getClassLoader();
				for (URL url : parent.getURLs()) {
					String su = url.toString();
					if (!(su.contains("ojdbc") || su.contains("orai18n")))
						cpList.add(url);
				}
				Logger.getInstance().debug("Using url "+cpList.get(0)+" for loading Oracle driver");
				URLClassLoader cl = new URLClassLoader(cpList.toArray(new URL[0]), null);
				
				Class classClob = cl.loadClass("oracle.sql.CLOB");
				createClob = classClob.getDeclaredMethod("createTemporary", java.sql.Connection.class,
					boolean.class, int.class);	
				clobSetString = classClob.getDeclaredMethod("setString", long.class, String.class);
				
				Class classOraPreparedStmt = cl.loadClass("oracle.jdbc.OraclePreparedStatement");
				setExecuteBatch = classOraPreparedStmt.getDeclaredMethod("setExecuteBatch", int.class);
				
				ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
				/* because of  javax.management.InstanceAlreadyExistsException */
				/* com.oracle.jdbc:type=diagnosability,name=sun.misc.Launcher$AppClassLoader@92e78c */
				Thread.currentThread().setContextClassLoader(cl); 
				drvRef.set((Driver) cl.loadClass("oracle.jdbc.OracleDriver").newInstance());
				String path = drvRef.get().getClass().getResource("/oracle/jdbc/OracleDriver.class") + "";
				Logger.getInstance().debug("OracleDriver path is "+path);
				Thread.currentThread().setContextClassLoader(savedClassLoader);
			}
		}
	}

	@Override
	protected void loadAutoConfig() {
		String host = Transport.getHostname();
		String repo = JobEventStats.getRepositoryName();
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		boolean wasError = false;
		
		try {
			conn = getConnection(connRef);
			
			st = SQLCache.prepareStatement(conn, "SELECT max(to_number(substr(column_name, 3, 10))) " +
					"as maxmk from user_tab_columns where table_name = 'EVENTS_T' " +
					"and column_name like 'MK%'");
			rs = st.executeQuery();
			boolean isDbReady = false;
			if (rs.next()) {
				int mk = rs.getInt(1);
				if (mk > 0 && mk < 255)
					maxMk.set(mk);
				isDbReady = mk > 0;
			}
			rs.close();
			SQLCache.closeStatement(conn, st);
			
			if (!isDbReady)
				OraBootstrap.setup(conn);
				
			st = SQLCache.prepareStatement(conn, "select param,value from PARAMS_T " +
				"where host=? and repo=?");	
			st.setString(1, host);
			st.setString(2, repo);
			rs = st.executeQuery();
			boolean entryExists = false;
			while (rs.next()) {
				entryExists = true;
				String p = rs.getString(1);
				String v = rs.getString(2);
				if (p!=null) {
					PEProc.setRuntimeProperty(p, v);
				}
			}
			if (!entryExists) {
				rs.close();
				SQLCache.closeStatement(conn, st);
				st = conn.prepareStatement("insert into PARAMS_T(host,repo, " +
					"param) values(?,?,?)");
				st.setString(1, host);
				st.setString(2, repo);
				st.setString(3, "NEEDS_CONFIG");
				st.executeUpdate();
				rs = null;
			}
			if (rs!=null)
				rs.close();
			rs = null;
			SQLCache.closeStatement(conn, st);
			st = null;
		}
		catch (Exception ex) {
			wasError = true;
			Logger.getInstance().debug("loadAutoConfig ERROR",ex);			
		}	
		finally {
			try {
				if (rs!=null)
					rs.close();				
			}
			catch (Exception erc) {}
			try {
				if (st!=null)
					st.close();				
			}
			catch (Exception esc) {}
			releaseConnection(conn, wasError);
		}		
	}

	@Override
	public Transport init() {
		return this;
	}
	
	@Override
	protected Connection getConnection(ThreadLocal<Connection> connRef) throws Exception {
		if (connRef.get()!=null)
			return connRef.get();
		else {
			installSealedDriver();
			Driver drv = drvRef.get();
			Connection conn = drv.connect(remoteAddress, new Properties());
			connRef.set(conn);
			SQLCache.registerNewConnection(conn);
			return conn;
		}
	}	
}
