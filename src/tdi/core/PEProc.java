/*
 * Copyright (c) 2010-2013 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import tdi.core.JobEventStats.PushPopEventAdapter;
import tdi.transport.SQLCache;
import tdi.transport.Transport;

import com.tibco.objectrepo.object.ObjectProvider;
import com.tibco.objectrepo.vfile.VFileFactory;
import com.tibco.pe.core.EngineHelper;
import com.tibco.pe.core.JobPoolEvent;
import com.tibco.pe.core.JobPoolListener;
import com.tibco.pe.core.WorkflowImpl;
import com.tibco.sdk.tools.MFileSink;

@ConfigParam(name="TDI_CONFIG_FILE", desc="Path to configuration file", value="./config.env")
public class PEProc extends com.tibco.pe.PEMain {
	
	private final static ConcurrentHashMap<Long, JobEventStats> jobListeners = new ConcurrentHashMap<Long, JobEventStats>();
	private final static String BANNER = "TDI (Tibco Discrete Instrumentation) v1.0.300, written by Jakub Jozwicki";
	private static WorkerThread wt = null;
	private final static ConcurrentHashMap<String,String> _props = new ConcurrentHashMap<String, String>();
	private final static ConcurrentHashMap<String,String> rtProps = new ConcurrentHashMap<String, String>();
	private final static AtomicReference<ConcurrentHashMap<String,String>> propsRef = new AtomicReference<ConcurrentHashMap<String,String>>();
	private final static Object initMutex = new Object();
	private final static Object destroyMutex = new Object();
	private static volatile JobPoolListener jobPoolListener = null;
		
	public PEProc() throws Exception {
		super();		
	}	

	public PEProc(Properties properties, MFileSink mfilesink) throws Exception {
		super(properties, mfilesink);
	}	

	public PEProc(Properties properties) throws Exception {
		super(properties);		
	}

	public PEProc(String[] as) throws Exception {
		super(as);		
	}

	public PEProc(VFileFactory vfilefactory, ObjectProvider objectprovider,
			ClassLoader classloader) throws Exception {
		super(vfilefactory, objectprovider, classloader);		
	}
	
	protected static void onDestroy() {
		synchronized (destroyMutex) {
			for (StackTraceElement sme : Thread.currentThread().getStackTrace()) {
				if (sme.toString().contains("com.tibco.pe.core.JobPool.destroy")) {
					Iterator<?> it = EngineHelper.getJobPool().getWorkflowLoader().getWorkflows();
					while (it.hasNext()) {
						try {
							((WorkflowImpl)it.next()).uninit();
						}
						catch (Exception exc) {}
					}
					EngineHelper.getJobPool().getWorkflowLoader().removeAll();
				}				
				break;
			}
		}		
	}
	
	private static JobPoolListener makeJobPoolListener() {
		return new JobPoolListener() {	
			
			public void stateChanged(final JobPoolEvent jpe) {				
				Runnable r = new Runnable() {					
					public void run() {
						try {
							JobEventStats tjl = jobListeners.get(jpe.jid);
							if (tjl!=null) {								
								tjl.stateChanged(jpe.getState() == JobEventStats.JOB_ACTIVE, 0);
							}
							else {
								tjl = new JobEventStats(jpe.jid);					
								jpe.addJobListener(tjl);
								jobListeners.put(jpe.jid, tjl);
							}							
						}
						catch (Exception exc) {
							Logger.getInstance().debug("stateChanged ERROR",exc);
						}						
					}					
				};
				if (!isInited()) {
					synchronized(initMutex) {
						r.run();
					}
				}
				else
					r.run();				
			}
			
			public void jobRemoved(final JobPoolEvent jpe) {
				Runnable r = new Runnable() {					
					public void run() {
						try {
							JobEventStats tjl = jobListeners.get(jpe.jid);
							if (tjl!=null) {
								tjl.jobRemoved();
								jobListeners.remove(jpe.jid);
								wt.onJobRemoved(tjl);
								jpe.removeJobListener(tjl);
							}
						}
						catch (Exception exc) {
							Logger.getInstance().debug("jobRemoved ERROR",exc);
						}						
					}
				};
				if (!isInited()) {
					synchronized(initMutex) {
						r.run();
					}
				}
				else 
					r.run();
			}	
			
			public void jobAdded(final JobPoolEvent jpe) {
				Runnable r = new Runnable() {
					
					public void run() {
						try {
							JobEventStats tjl = jobListeners.get(jpe.jid);
							if (tjl==null) {
								tjl = new JobEventStats(jpe.jid);
								jpe.addJobListener(tjl);
								jobListeners.put(jpe.jid, tjl);
							}
						}
						catch (Exception exc) {
							Logger.getInstance().debug("jobAdded ERROR",exc);
						}
						
					}
				};
				if (!isInited()) {
					synchronized(initMutex) {
						r.run();
					}
				}
				else
					r.run();
			}
		};		
	}

	protected static boolean isInited() {
		return EngineHelper.getProperty("TDI_INITED", "0").equals("1");
	}

	public static void main(String args[])
    {
        isMain = true;
        try
        {
        	for (Entry<Object,Object> en : System.getProperties().entrySet()) {
        		if (en.getValue()!=null)
        			_props.put(en.getKey().toString(), en.getValue().toString());
        	}
        	System.out.println("==== Started with arguments ====");
        	for (int i=0; i < args.length; i++) {
        		System.out.print(args[i]+", ");        		
        	}
        	System.out.println();
        	new PEProc(args);        	
        }
        catch(Throwable t)
        {
            t.printStackTrace();
            System.exit(-1);
        }
    }	
	
	public void start() throws Exception {
		synchronized(initMutex) {
			super.start();
			tdi.api.INSTANCE.instrument(null);			
		}
	}
	
	public final static void TibcoPropertiesBugWorkaround() {
		if (System.getProperty("line.separator")==null) {
			Properties p = System.getProperties();
			System.setProperties(null);
			if (p!=null)
				System.getProperties().putAll(p);			
		}		
	}

	private static void instrument() throws Exception {
		if (!isInited()) {
			TibcoPropertiesBugWorkaround();		
			XmlContentTracerCompanion.init();
			if (jobPoolListener == null)
				jobPoolListener = makeJobPoolListener();
			EngineHelper.getJobPool().addJobPoolListener(jobPoolListener);
			EngineHelper.injectCanaryWorkflow();
			if (wt==null || !wt.isAlive()) {				
				Logger.getInstance().debug("Using "+BANNER);
				(wt = new WorkerThread(jobListeners)).start();		
				String jarName = null;
				try {
					URL jarUrl = PEProc.class.getResource("/"+PEProc.class.getName().replace('.', '/')+".class");
					if (jarUrl!=null) {
						jarName = jarUrl.getPath();
						int i = jarName.lastIndexOf("!");
						if (i>0)
							jarName = jarName.substring(0, i);
					}
				}
				catch (Exception e) {}
				Transport.loadAutoCfg();
			}
			setInited(1);			
		}
	}	
	
	private static void setInited(int state) {
		for (int i=0; i < 30; i++) {
			if (EngineHelper.setProperty("TDI_INITED", state+""))
				break;
		}		
	}

	private static void putProps(Reader r, ConcurrentHashMap<String, String> propsMap) throws IOException {
		BufferedReader br = new BufferedReader(r);
		String line = null;
		while ((line=br.readLine())!=null) {
			String[] kv = line.split("=");
			if (kv!=null && kv.length==2) {
				if (!kv[0].trim().startsWith("#"))
					propsMap.put(kv[0].trim(), kv[1].trim());
			}
		}
		br.close();
	}

	public static Map<String, String> loadPropertiesFromFile() {
		TibcoPropertiesBugWorkaround();
		boolean needsInit = propsRef.compareAndSet(null, new ConcurrentHashMap<String, String>());
		ConcurrentHashMap<String, String> propsFromFile = propsRef.get();
		if (needsInit) {
			File f = new File(System.getProperty("TDI_CONFIG_FILE", "./config.env"));
			if (!f.exists())
				f = new File("./config.env");
			@SuppressWarnings("unused")
			String aPath = null;
			try {
				aPath = f.getAbsolutePath();
			}
			catch (Exception e) {
				aPath = "working dir";				
			}
			//Logger.getInstance().debug("Looking for config file in "+aPath);
			if (f.exists() && f.canRead()) {
				FileReader fr = null;
				try {
					fr = new FileReader(f);
					putProps(fr, propsFromFile);
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
			InputStream is = PEProc.class.getResourceAsStream("/config.env");
			if (is!=null) {
				try {
					InputStreamReader isr = new InputStreamReader(is);
					putProps(isr, propsFromFile);
					isr.close();
					is.close();
				}
				catch (Exception e) {
					if (is!=null) {
						try {
							is.close();
						}
						catch (Exception exc) {}
					}
				}
			}
		}
		return propsFromFile;
	}	
	
	private final static void readAutoCfgFile() {
		if (rtProps.putIfAbsent("AUTO_CFG_READ", "1")==null) {
			InputStream is = null;
			try {
				is = PEProc.class.getResourceAsStream("/auto.cfg");
				if (is!=null) {
					byte[] buff = new byte[4096];
					int n = is.read(buff);
					if (n>0) {
						String address = new String(buff, 0, n).trim();
						PEProc.setRuntimeProperty("TSI_ADDRESS", address);
						if ("void".equals(address)) {
							Transport.noTransport();
						}
						Logger.getInstance().debug("Autoconfig file has been read");						
					}
					else {
						Logger.getInstance().debug("Autoconfig file is invalid");
					}
				}
				else {
					Logger.getInstance().debug("Autoconfig file not found");
				}			
			}
			catch (Exception e) {
				Logger.getInstance().debug("readAutoCfg ERROR",e);
			}
			finally {
				if (is!=null) {
					try {
						is.close();
					}
					catch (Exception exc) {}
				}				
			}
		}
	}
	
	public static String getRuntimeProperty(String p, String def) {	
		if (rtProps.putIfAbsent("AUTO_CFG_DONE", "1")==null) {
			readAutoCfgFile();
			Logger.getInstance().debug("Trying to autoconfig from transport");
			Transport.loadAutoCfg();
		}
		return getStaticProperty(p, def);
	}
	
	public static String getStaticProperty(String p, String def) {
		String val = loadPropertiesFromFile().get(p);
		readAutoCfgFile();
		String rtVal = rtProps.get(p);
		if (rtVal!=null)
			return rtVal;
		if (val!=null)
			return val;
		val = _props.get(p);
		if (val!=null)
			return val;
		val = System.getenv(p);
		if (val!=null)
			return val;
		return def;
	}
	
	public static ValueHolder<Boolean> getVolatileBooleanProperty(final String p,
			final String v)
	{
		return new ValueHolder<Boolean>() {			
			private static final long serialVersionUID = 1L;
			public Boolean get() {
				String val = getRuntimeProperty(p, v).toLowerCase();
				return val.equals("true") || val.equals("yes") || val.equals("1") || val.equals("on");
			}
		};
	}

	public static ValueHolder<Long> getVolatileLongProperty(final String p,
			final String v)
	{
		return new ValueHolder<Long>() {
			private static final long serialVersionUID = 1L;
			public Long get() {
				String val = getRuntimeProperty(p, v).toLowerCase();
				return Long.valueOf(val);
			}
		};
	}
	
	public static ValueHolder<String> getVolatileStringProperty(final String p,
			final String v)
	{
		return new ValueHolder<String>() {
			private static final long serialVersionUID = 1L;
			public String get() {
				return getRuntimeProperty(p, v).toLowerCase();				
			}
		};
	}

	public static void setRuntimeProperty(String p, String v) {
		if (v!=null)
			rtProps.put(p, v);		
	}
	
	private static void instrumentFromBW(Connection bwTdiConnection) throws Exception {
		SQLCache.overrideConnection(bwTdiConnection);
		instrument();
	}
	
	public static void instrumentLate(Connection bwTdiConnection) {
		try {
			instrumentFromBW(bwTdiConnection);
			if (EngineHelper.getJobPool()!=null) {
				long[] ids = EngineHelper.getJobPool().getJobIds();
				if (ids!=null) {
					for (long jid : ids) {
						EngineHelper.addLateJobListener(jid);
					}
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void addEvent(JobEventStats evt) {
		if (wt!=null)
			wt.addEvent(evt);
	}

	public static void dispatchPopEvents(LinkedList<JobEventStats> events) {
		HashMap<String,String> procDefs = new HashMap<String,String>();
		Iterator<JobEventStats> it = events.iterator();
		while (it.hasNext()) {
			JobEventStats current = it.next();		
			PushPopEventAdapter ea = current.toPushPopEventAdapter();
			if ("def".equals(ea.getSubtype())) {
				it.remove();
				procDefs.put(ea.getKey(), ea.getContent());
			}
		}
		if (!procDefs.isEmpty())
			tdi.api.INSTANCE.UpdateProcessDefinition(procDefs);
		
		for (JobEventStats evt : events) {
			PushPopEventAdapter ea = evt.toPushPopEventAdapter();
			if ("hib".equals(ea.getSubtype())) {
				try {
					tdi.api.INSTANCE.RestoreProcess(ea.getContent());
				}
				catch (Exception e) {
					Logger.getInstance().debug("Cannot restore process", e);
				}
			}
		}		
	}
}
