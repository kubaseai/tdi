/*
 * Copyright (c) 2010-2011 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import tdi.core.JobEventStats;
import tdi.transport.HSQLTransport;

public class Benchmark {
	
	private final static LinkedList<JobEventStats> list = new LinkedList<JobEventStats>();
	private static AtomicInteger cnt = new AtomicInteger();
	private static long time0 = System.currentTimeMillis();
	
	static {
		for (int i=0; i < 1000; i++) {
			list.add(new JobEventStats(i) {

				private static final long serialVersionUID = 1L;

				@Override
				public String getRepoName() {
					return "repo-benchmark";
				}

				@Override
				public int getCreated() {
					return this.hashCode()%1000;
				}

				@Override
				public int getCompleted() {
					return this.hashCode()%1000;
				}

				@Override
				public int getRunning() {
					return this.hashCode()%1000;
				}

				@Override
				public long getStart() {
					return System.currentTimeMillis()- this.hashCode()%1000;
				}

				@Override
				public long getDelayed() {
					return this.hashCode()%1000;
				}

				@Override
				public String getProcessName() {
					return "bench.process";
				}

				@Override
				public long getEnd() {
					return System.currentTimeMillis();
				}				
			});			
		}
	}
	
	private final static void read() {
		Connection conn = null;
		Statement st = null;
		try {
			new tdi.sql.JDBCDriver();
			conn = DriverManager.getConnection("tdi:localhost:8192", "sa", "");
			st = conn.createStatement();
			st.execute("select count(*) from USAGE_T where cpu > "+Thread.currentThread().getId());
		}
		catch (Exception e) {
			e.printStackTrace();			
		}
		finally {
			try {
				if (st!=null)
					st.close();				
			}
			catch (Exception esc) {}
		}
		try {
			if (conn!=null)
				conn.close();
		}
		catch (Exception ecc) {}
	}
	
	private final static void write() {
		HSQLTransport.deliverEvents("stats", list);
	}
	
	public final static void main(String[] args) {
		for (int i=0; i < 6; i++) {
			new Thread() {
				public void run() {
					for (int j=0; j < 1000; j++) {
						if (j%3==0)
							read();
						else
							write();
						if (j%100==1)
							System.out.println(j+" in "+Thread.currentThread().getId());
					}	
					if (cnt.incrementAndGet() >= 6)
						System.err.println("Time is: "+(System.currentTimeMillis()-time0));
				}
			}.start();
		}
	}
}
