/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Logger {
	
	private static class LogWriter {				
		public void write(String msg) {
			System.err.println("(TDI) "+msg);
		}
	}
	
	public final static Logger LOGGER = new Logger();
	private LogWriter logWriter = null;	
	private AtomicBoolean appendersDone = new AtomicBoolean(false);
		
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Logger() {
		try {
			final Class clazz = Class.forName("org.apache.log4j.Logger");
			final Object objLoggerBW = clazz.getDeclaredMethod("getLogger", String.class).invoke(null, "bw.logger");
			final Method debug = clazz.getMethod("info", Object.class);
															
			logWriter = new LogWriter() {
				private void singleAppender() {
					if (appendersDone.compareAndSet(false, true)) {					
						try {
							clazz.getSuperclass().getDeclaredMethod("removeAppender", String.class).invoke(objLoggerBW, "tibco_bw_log_console");
						}
						catch (Throwable t) {}
					}
				}
				public void write(String s) {
					boolean done = false;
					try {
						singleAppender();
						debug.invoke(objLoggerBW, "[TDI] "+s);
						done = true;												
					}
					catch (Exception e) {}
					if (!done)
						super.write(s);
				}
			};
		}
		catch (Exception e) {
			logWriter = new LogWriter();
		}
	}
	
	public static Logger getInstance() {
		return LOGGER;
	}
	
	public void debug(String msg) {
		logWriter.write(msg);
	}
	
	public void debug(String msg, Throwable t) {
		debug(describeError(msg, t));
	}	

	protected static String describeError(String msg, Throwable t) {
		StringWriter sw = new StringWriter();
		sw.append(msg).append(": ");
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		SQLException sqle = (SQLException) ((t instanceof SQLException) ? t : null);
		while ((t=t.getCause())!=null)
			t.printStackTrace(pw);
		while (sqle!=null && (sqle=sqle.getNextException())!=null)
			sqle.printStackTrace();
		pw.flush();
		sw.flush();
		String res = (sw.getBuffer().toString());
		try {
			sw.close();
		}
		catch (IOException e) {}
		pw.close();
		return res;		
	}	
}
