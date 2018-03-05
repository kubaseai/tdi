/*
 * Copyright (c) 2010-2013 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import tdi.core.JobEventStats;
import tdi.core.JobEventStats.StepEntry;
import tdi.core.Logger;
import tdi.core.PEProc;
import tdi.core.ProtectingClassLoader;
import tdi.core.ValueHolder;
import tdi.core.activities.JVMStats;

public class HSQLTransport extends Transport {
	
	private final static ValueHolder<Boolean> skipMessage =
		PEProc.getVolatileBooleanProperty("SKIP_MK_MESSAGE", "false");
	
	protected ThreadLocal<Connection> connRef = new ThreadLocal<Connection>();
	
	private void initLocalServer() {
		Logger.getInstance().debug("Local transport server requested at "+remoteAddress);
		remoteAddress = HSQLServer.create(remoteAddress);	
		try {
			Thread.sleep(7000);
		} 
		catch (InterruptedException e) {}
		/* workaround: DB 2.3.2 binds to hostname */
		String[] hp = remoteAddress.split("\\:");
		remoteAddress = Transport.getHostname() + ":" + hp[1];
	}
	
	private void initOrReuseLocalServer(Integer port) {
		port = detectLocalSeverPort(port);
		if (port == null) {
			initLocalServer();
		}
		else {
			remoteAddress = "localhost:"+port;
			Logger.getInstance().debug("Local transport server found running at "+remoteAddress);
		}
	}
	
	private Integer detectLocalSeverPort(Integer basePort) {
		Logger.getInstance().debug("Searching for active transport server port");
		int[] portBases = { basePort!=null ? basePort : 8192 };
		for (int base : portBases) {
			for (int p=base; p <= base+2; p++) {
				Connection conn = null;
				try {
					Logger.getInstance().debug("Trying "+p);
					conn = new tdi.sql.JDBCDriver().connect("tdi:"+Transport.getHostname()+":"+p+"", null);
					conn.setAutoCommit(false);					
					return p;
				}
				catch (Exception e) {}
				finally {
					if (conn!=null) {
						try {
							conn.close();
						}
						catch (Exception exc) {}
					}
				}				
			}			
		}
		return null;
	}
			
	public Transport init() {
		AtomicBoolean discovered = new AtomicBoolean(false);
		if ("void".equals(remoteAddress))
			return new VoidTransport();
		if (remoteAddress.startsWith("auto:")) {
			Integer udpPort = 8192;
			try {
				udpPort = Integer.valueOf(remoteAddress.substring(5));
			}
			catch (Exception e) {}
			remoteAddress = HSQLDiscovery.findActiveNode(udpPort, 10000, discovered);
			if (discovered.get())
				Logger.getInstance().debug("Discovery returned address "+remoteAddress);									
		}
		if (remoteAddress.startsWith("localhost:") || remoteAddress.startsWith("127.0.0.1:")) {
			initOrReuseLocalServer(Integer.valueOf(remoteAddress.split(":")[1]));
		}
		return this;
	}
	
	private final static Long getLongParam(JobEventStats je, String p) {
		String val = je.getMetrics().get(p);
		if (val!=null) {
			try {
				if ("true".equals(val))
					return 1L;
				return Long.valueOf(val);
			}
			catch (Exception e) {}
		}
		return null;
	}
	
	protected void setClob(PreparedStatement ps, int idx, String val) throws SQLException {
		ps.setString(idx, val);
	}
	
	protected void setVarchar2(PreparedStatement ps, int idx, String val, int limit) throws SQLException {
		ps.setString(idx, val);
	}
	
	protected void setupBatch(PreparedStatement ps) throws SQLException {
		;
	}
	
	protected void addBatch(PreparedStatement ps) throws SQLException {
		ps.addBatch();
	}
	
	protected void executeBatch(PreparedStatement ps) throws SQLException {
		ps.executeBatch();
	}
	
	protected void insertEventsIntoDb(LinkedList<JobEventStats> events, Connection conn) throws Exception
	{
		PreparedStatement st = null;
		PreparedStatement stData = null;
		String host = Transport.getHostname();
		try {
			if (!events.isEmpty()) {
				StringBuilder sql = new StringBuilder("insert into EVENTS_T(host,repo,proc,job,cr,fn,rn,fl,tc,st," +
					"d,en,wt,et,ec,type,jts,jexp,jdc,metrics,em," +
					"markers_defs, markers, markers_msgs");
				String data = "insert into EVENTS_DATA(host, repo, proc, job, kind, data, input, output, v1, v2, v3) "+
					"values(?,?,?,?, ?,?,?,?, ?,?,?)";
				
				for (int i=0; i < maxMk.get(); i++)
					sql.append(", mk").append(i+1);			
				sql.append(") values(?,?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?");
				
				for (int i=0; i < maxMk.get(); i++)
					sql.append(",?");
				sql.append(")");	
				
				st = SQLCache.prepareStatement(conn, sql.toString());
				setupBatch(st);
				stData = SQLCache.prepareStatement(conn, data);
				setupBatch(stData);
													
				for (JobEventStats je : events) {
					st.setString(1, host);
					st.setString(2, je.getRepoName());
					st.setString(3, je.getProcessName());
					st.setLong(4, je.getJobId());
					st.setLong(5, je.getCreated());
					st.setLong(6, je.getCompleted());
					st.setInt(7, je.getRunning());
					st.setInt(8, je.getFlowLimit());
					st.setInt(9, je.getThreadCount());
					st.setLong(10, je.getStart());
					st.setLong(11, je.getDelayed());
					st.setLong(12, je.getEnd());
					st.setLong(13, je.getSuspendTime());
					st.setLong(14, je.getEvalTime());
					st.setLong(15, je.getErrorCount());
					st.setString(16, je.getEventType());
					Long jts = getLongParam(je, "JMSTimestamp");
					Long jexp = getLongParam(je, "JMSExpiration");
					Long jdc = getLongParam(je, "JMSXDeliveryCount");
					if (jdc==null)
						jdc = getLongParam(je, "JMSRedelivered");
					
					if (jts!=null)
						st.setLong(17, jts);
					else
						st.setNull(17, java.sql.Types.INTEGER);
					
					if (jexp!=null)
						st.setLong(18, jexp);
					else
						st.setNull(18, java.sql.Types.INTEGER);
					
					if (jdc!=null)
						st.setLong(19, jdc);
					else
						st.setNull(19, java.sql.Types.INTEGER);
					String metrics = je.getMetricsString();
					setClob(st, 20, metrics); 
					setClob(st, 21, je.getErrorMessages());
										
					setClob(st, 22, je.getMarkersDefsRaw());
					StringBuilder markers = je.getMarkers();
					StringBuilder markerMessages = je.getMarkersMessages();
					int stepsWithMessages = 0;
					int steps = 0;
					for (StepEntry step : je.getSteps()) {
						if (step.kind==null) {
							steps++;
							if (step.input!=null || step.output!=null) {
								stepsWithMessages++;
							}
						}
					}
					if (stepsWithMessages > 0 && steps > 0)
						markerMessages = new StringBuilder(); // messages are inside step records, do not duplicate
					setClob(st, 23, markers.length()>0 ? markers.toString() : null);
					setClob(st, 24, markerMessages.length()>0 ? markerMessages.toString() : null);
					
					String[] mk = je.getGlobalMarkers();
					for (int i=0; i < maxMk.get(); i++) {
						String v = mk!=null && i >= 0 && i < mk.length ? mk[i] : null;
						if (v!=null)
							setVarchar2(st, 25+i, v, 4000);
						else
							st.setNull(25+i, java.sql.Types.VARCHAR);
					}
					addBatch(st);
				}
				executeBatch(st);
				
				boolean hasSteps = false;
				for (JobEventStats je : events) {
					for (JobEventStats.StepEntry step : je.getSteps()) {
						long times[] = step.getTimes();
						stData.setString(1, host);
						stData.setString(2, je.getRepoName());
						stData.setString(3, je.getProcessName());
						stData.setLong(4, je.getJobId());
						stData.setString(5, step.kind);
						stData.setString(6, step.getDescription());
						if (!skipMessage.get()) {
							setClob(stData, 7, step.input);
							setClob(stData, 8, step.input!=null && step.input.equals(step.output) ? null : step.output);
						}
						else {
							setClob(stData, 7, null);
							setClob(stData, 8, null);
						}
						for (int i=0; i < times.length; i++)
							stData.setLong(9+i, times[i]);
						addBatch(stData);
						hasSteps = true;
					}					
				}
				if (hasSteps)
					executeBatch(stData);
				
				conn.commit();
				st.clearBatch();
				st.clearParameters();
				SQLCache.closeStatement(conn, st);
				st = null;
				
				if (hasSteps) {
					stData.clearBatch();
					stData.clearParameters();
				}				
				SQLCache.closeStatement(conn, stData);
				stData = null;
			}
			
			st = SQLCache.prepareStatement(conn, "insert into USAGE_T(host,repo,cpu,mem,gc," +
					"st) "+
					"values(?,?, ?,?,?, ?)");
			st.setString(1, host);
			st.setString(2, JobEventStats.getRepositoryName());
			long s = System.currentTimeMillis();
			double cpu = JVMStats.getJvmCpuUsage();
			double mem = JVMStats.getPercentMemUsage();
			double gc = JVMStats.getPercentGcUsage();
			st.setDouble(3, cpu);
			st.setDouble(4, mem);
			st.setDouble(5, gc);
			st.setLong(6, s);			
			st.executeUpdate();
			conn.commit();
			st.clearParameters();
			SQLCache.closeStatement(conn, st);
			st = null;
		}
		catch (Exception sqle) {
			String msg = sqle.getMessage() + "";
			if (!msg.contains("violation"))
				throw sqle;			
		}
		finally {
			SQLCache.closeStatement(conn, st);
		}
	}

	protected Connection getConnection(final ThreadLocal<Connection> connRef) throws SQLException, Exception {
		if (connRef.get()!=null)
			return connRef.get();
		else {
			ProtectingClassLoader.seal(new ProtectingClassLoader.Closure() {				
				@Override
				public Object invoke() throws Exception {
					Exception exc = null;
					for (int i=0; i < 10; i++) {
						try {
							Connection conn = tdi.sql.JDBCDriver.getConnection("tdi:"+remoteAddress+"", new Properties());
							connRef.set(conn);
							SQLCache.registerNewConnection(conn);
							return null;
						}
						catch (Exception e) {
							exc = e;
							Thread.sleep(3000);
						}
					}
					if (exc!=null)
						throw exc;
					throw new Exception("Connection is null");
				}
			});
			return connRef.get();
		}
	}
	
	protected void releaseConnection(Connection conn, boolean forceClose) {
		try {
			if (SQLCache.deregisterConnection(conn, forceClose)) {
				connRef.set(null);
				if (conn!=null)
					conn.close();
			}
		}
		catch (Exception ecc) {
			Logger.getInstance().debug("Error on releaseConnection", ecc);
		}		
	}
	
	@Override
	public void sendEvents(LinkedList<JobEventStats> events) throws Exception {
		boolean success = false;
		try {
			Connection conn = getConnection(connRef);
			conn.setAutoCommit(false);			
			insertEventsIntoDb(events, conn);
			success = true;
		}
		catch (Exception ex) {
			throw ex;
		}	
		finally {
			releaseConnection(connRef.get(), !success);			
		}
	}

	@Override
	public void uploadProcessesIfNeeded(
			HashMap<String, String[]> retrieveProcessFiles) throws Exception {
		
		boolean success = false;
		try {
			Connection conn = getConnection(connRef);
			conn.setAutoCommit(false);			
			updateProcesses(conn, retrieveProcessFiles);
			success = true;
		}
		catch (Exception ex) {
			throw ex;
		}	
		finally {
			releaseConnection(connRef.get(), !success);			
		}				
	}
	
	public void updateProcesses(Connection conn,
		HashMap<String, String[]> retrieveProcessFiles) throws Exception
	{		
		PreparedStatement st = null;
		String host = Transport.getHostname();
		String repo = JobEventStats.getRepositoryName();
		try {
			if (!retrieveProcessFiles.isEmpty()) {
				StringBuilder sql = new StringBuilder("insert into EVENTS_DATA(host,repo,proc,kind,data,input)");
				sql.append(" select ?,?,?,?, ?,? from dual where not exists "+
				"(select 'X' from EVENTS_DATA where host=? and repo=? and proc=? and kind=?)");
				st = SQLCache.prepareStatement(conn, sql.toString());
				setupBatch(st);
				
				for (Entry<String,String[]> entry : retrieveProcessFiles.entrySet()) {
					String fname = entry.getKey();
					String content = entry.getValue()[0];
					String hash = entry.getValue()[1];
					
					st.setString(1, host);
					st.setString(2, repo);
					st.setString(3, fname);
					st.setString(4, hash);
					st.setString(5, System.currentTimeMillis()+"");					
					setClob(st, 6, content);
					st.setString(7, host);
					st.setString(8, repo);
					st.setString(9, fname);
					st.setString(10, hash);
					addBatch(st);
				}				
				executeBatch(st);				
				conn.commit();
				st.clearBatch();
				st.clearParameters();
				SQLCache.closeStatement(conn, st);
				st = null;				
			}			
		}
		finally {
			SQLCache.closeStatement(conn, st);
		}				
	}

	protected LinkedList<JobEventStats> retrieveEventsImpl(Connection conn) throws Exception {
		LinkedList<JobEventStats> list = new LinkedList<JobEventStats>();
		PreparedStatement st = null;
		PreparedStatement stDel = null;
		try {
			st = SQLCache.prepareStatement(conn, "select id,type,key,value from POP_T "+
				"where host = ? and repo = ?");
			st.setString(1, Transport.getHostname());
			st.setString(2, JobEventStats.getRepositoryName());
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				JobEventStats evt = JobEventStats.createPushPopEvent(rs.getLong(1), "pop", rs.getString(2),
					rs.getString(3), rs.getString(4));
				list.add(evt);				
			}
			rs.close();
			rs = null;
			SQLCache.closeStatement(conn, st);
			conn.commit();
			st = null;		
			
			if (!list.isEmpty()) {
				stDel = SQLCache.prepareStatement(conn, "delete from POP_T where id = ?");
				setupBatch(stDel);
				for (JobEventStats evt : list) {
					stDel.setLong(1, evt.getJobId());
					addBatch(stDel);
				}				
				executeBatch(stDel);				
				conn.commit();
				stDel.clearBatch();
				stDel.clearParameters();
				SQLCache.closeStatement(conn, stDel);
				stDel = null;
			}
		}
		finally {
			SQLCache.closeStatement(conn, st);
			SQLCache.closeStatement(conn, stDel);
		}
		return list;
	}

	@Override
	protected LinkedList<JobEventStats> retrieveEvents() throws Exception {
		boolean success = false;
		try {
			Connection conn = getConnection(connRef);
			conn.setAutoCommit(false);			
			LinkedList<JobEventStats> list = retrieveEventsImpl(conn);
			success = true;
			return list;
		}
		catch (Exception ex) {
			throw ex;
		}	
		finally {
			releaseConnection(connRef.get(), !success);			
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
}
