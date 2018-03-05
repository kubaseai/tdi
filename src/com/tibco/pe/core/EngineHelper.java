/*
 * Copyright (c) 2010-2011,2017 Jakub Jozwicki. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package com.tibco.pe.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xml.sax.InputSource;

import tdi.core.JobEventStats;
import tdi.core.JobExtractor;
import tdi.core.Logger;
import tdi.core.NodeTextExtractor;
import tdi.core.ProcessExtractor;

import com.tibco.bw.store.RepoAgent;
import com.tibco.pe.model.ActivityReport;
import com.tibco.pe.model.ProcessModel;
import com.tibco.pe.model.ProcessReport;
import com.tibco.pe.plugin.DataModel;
import com.tibco.xml.datamodel.XiNode;
import com.tibco.xml.datamodel.XiParserFactory;
import com.tibco.xml.xdata.xpath.Variable;
import com.tibco.xml.xdata.xpath.VariableList;

public class EngineHelper {
	
	private static JobPool jobPool = null;
	@SuppressWarnings("rawtypes")
	private final static Comparator STRING_COMPARATOR = new Comparator() {

		public int compare(Object o1, Object o2) {
			String s1 = o1!=null ? o1.toString() : "";
			String s2 = o2!=null ? o2.toString() : "";
			return s1.compareTo(s2);
		}
	}; 
	
	public static JobPool getJobPool() {
		try {
			if (jobPool!=null)
				return jobPool;
			return Engine.pool;
		}
		catch (Throwable t) {
			for (Field f : Engine.class.getDeclaredFields()) {
				if (f.getType().getName().endsWith("JobPool")) {
					f.setAccessible(true);
					try {
						return jobPool = (JobPool) f.get(null);
					}
					catch (Throwable te) {
						throw new RuntimeException("Cannot access JobPool", te);
					}
				}
			}
			throw new RuntimeException("Cannot access JobPool", t);
		}
	}
	
	public final static Object execute(Object on, String method, Object[] args) {
		for (Method m : on.getClass().getDeclaredMethods()) {
			if (m.getName().equals(method)) {
				m.setAccessible(true);
				try {
					return m.invoke(on, args);
				}
				catch (Exception e) {
					throw new RuntimeException("Excecution failed: "+e.getMessage(), e);
				}
			}
		}
		return null;
	}
	
	protected final static void setField(Object on, String name, Object value) {
		Class<?> c = on.getClass();
		while (c!=null) {
			try {
				Field f = c.getDeclaredField(name);
				if (f!=null) {
					f.setAccessible(true);
					f.set(on, value);
					break;
				}				
			}
			catch (Exception e) {}
			finally {
				c = c.getSuperclass();
			}
		}
	}
	
	protected final static Object getFieldByTypeName(Object on, Class<?> clazz, String name) {
		if (on!=null)
			clazz = on.getClass();
		for (Field f : clazz.getDeclaredFields()) {
			if (f.getType().getName().endsWith(name)) {
				f.setAccessible(true);
				try {
					return f.get(on);
				}
				catch (Throwable te) {
					throw new RuntimeException("Cannot access "+name, te);
				}
			}
		}
		return null;
	}
	
	public final static Object getFieldByName(Object on, Class<?> clazz, String name, boolean wantAccessor) {
		if (on!=null)
			clazz = on.getClass();
		for (Field f : clazz.getDeclaredFields()) {
			if (f.getName().equals(name)) {
				f.setAccessible(true);
				try {
					if (wantAccessor)
						return f;
					return f.get(on);
				}
				catch (Throwable te) {
					throw new RuntimeException("Cannot access "+name, te);
				}
			}
		}
		return null;
	}
	
	public final static Object getFieldByName(Object on, Class<?> clazz, String name) {
		return getFieldByName(on, clazz, name, false);
	}
	
	protected final static void setFieldByTypeName(Object on, String name, Object value) {
		if (on!=null) {
			for (Field f : on.getClass().getDeclaredFields()) {
				if (f.getType().getName().endsWith(name)) {
					f.setAccessible(true);				
					try {
						if ((f.getModifiers() & Modifier.FINAL)!=0) {
							Field modifiersField = Field.class.getDeclaredField("modifiers");
						    if (modifiersField!=null) {
						    	modifiersField.setAccessible(true);
						    	modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
						    }
						    else /* wow, someone at Oracle secured this hole */;					    
						}				    
						f.set(on, value);
					}
					catch (Throwable te) {
						throw new RuntimeException("Cannot access "+name, te);
					}
				}
			}
		}
	}
	
	public static VariableList getJobVariables(long jid, int trackId) {
		try {
			Method getAttributes = Class.forName("com.tibco.pe.core.Job").getDeclaredMethod("getAttributes", new Class<?>[] { int.class });
			getAttributes.setAccessible(true);
			if (EngineHelper.getJobPool().findJob(jid)!=null)
				return (VariableList) getAttributes.invoke(EngineHelper.getJobPool().findJob(jid), trackId);
			return null;
		}
		catch (Throwable t) {
			throw new RuntimeException("Cannot access job variables: "+t.getMessage(), t);
		}
	}
	
	public static VariableList getJobData(long jid) {
		Job job = getJobPool().findJob(jid);
		return job!=null ? job.getCurrentAttributes() : null;		
	}
	
	public static String getActivityClassName(long jid, String an) {
		DataModel av = getActivity(jid, an);
		return av!=null ? av.getClassName() : "";
	}
	
	public static DataModel getActivity(long jid, String an) {
		ProcessReport pr = getProcessReport(jid);
		ActivityReport ar = pr!=null ? pr.getActivity(an) : null;
		return (ar!=null ? ar.getActivity() : null);
	}
	
	public static RepoAgent getRepoAgent() {
		return Engine.getRepoAgent();
	}
	
	public static ProcessReport getProcessReport(long jid) {
		if (getJobPool().findJob(jid)!=null && getJobPool().findJob(jid).getActualWorkflow()!=null)
			return getJobPool().findJob(jid).getActualWorkflow().getProcessReport();
		return null;
	}
	
	public static ProcessStarter getProcessStarter(long jid) {
		Job job = getJobPool().findJob(jid);
		return job!=null && job.getActualWorkflow()!=null ? job.getActualWorkflow().getStarter() : null;
	}
	
	public static JobContainer getJobWithinContainer(long jid) {
		return new JobContainer(getJobPool().findJob(jid));
	}
	
	private static void setJobVariable(Job job, String name, String xml) throws Exception {
		job.getCurrentAttributes().setVariable(name, 
			new Variable(XiParserFactory.newInstance().parse(
				new InputSource( new StringReader(xml) )						
			))
		);
	}
	
	private static void setMinimalProcessContext(Job job, long jid, String tracking) throws Exception {
		setJobVariable(job, "_processContext", 
			"<ProcessContext>" +
				"<ProcessId>"+jid+"</ProcessId><ProjectName/><EngineName/>" +
				"<RestartedFromCheckpoint>true</RestartedFromCheckpoint>" +	
				(tracking!=null ? "<TrackingInfo>"+tracking+"</TrackingInfo>" : "") +
			"</ProcessContext>");
	}
	
	public static JobContainer hibernateJob(long jid) throws Exception {
		JobContainer jc = getJobWithinContainer(jid);
		Variable var = EngineHelper.getJobData(jid).getVariable("_processContext");
		if (var!=null) {
			XiNode node = var.getValue().getFirstChild().getFirstChild();
			do {
				if ("TrackingInfo".equals(node.getName().getLocalName())) {
					String val = NodeTextExtractor.getText(node);
					if (val!=null && val.startsWith("HIBERNATED_RESUMED")) {
						jc.setHibernated(false);
						return jc;
					}
				}
				else
					node = node.hasNextSibling() ? node.getNextSibling() : null;
			}
			while (node!=null);
		}
		if (jc.job != null) {
			getJobPool().hibernateJob(jc.job, "End", null, null, null);
			jc.setHibernated(true);
			getJobPool().killJob(jid+"");
		}
		return jc;
	}	
	
	public static void resumeHibernatedJob(long jid) throws Exception {
		resumeHibernatedJob(jid, null);
	}
	
	public static void resumeHibernatedJob(long jid, JobData override) throws Exception {
		boolean dataKnown = false;
		RecoverableJobData[] rjd = getJobPool().getHibernatedJobs();
		if (rjd!=null) {
			for (RecoverableJobData r : rjd) {
				if (r.jobId == jid)
					dataKnown = true;
					break;
			}
		}
		String token = "HIBERNATED_RESUMED_"+System.currentTimeMillis();
		if (!dataKnown || override!=null) {
			JobData jd = override!=null ? override : 
				getJobPool().hibernateDataManager.load(jid, JobPool.getFTName(), true);
			Workflow wf = getJobPool().getWorkflow(jd.wf);
			if (wf == null) {
				ProcessModel pm = ProcessExtractor.retrieveProcessModel(jd.wf);
				if (pm!=null) {
					Engine.getModels().add(pm);
					wf = getJobPool().getWorkflowLoader().loadWorkflow(pm);
				}
				if (wf == null)
					throw new RuntimeException("Process "+jd.wf+" is not loaded by BW Engine");
			}
			Job job = new Job("Job-"+jid+"", wf);
			setMinimalProcessContext(job, jid, token);
			jd.fillJob(job, getJobPool());			
			getJobPool().addJob(job, "Hibernation", true);			
		}
		else {
			setMinimalProcessContext(getJobPool().findJob(jid), jid, token);			
			getJobPool().restartHibernatedJob(jid);	
		}
	}
	
	public static JobExtractor getExtractedJob(long jid) throws Exception {
		Job job = getJobPool().findJob(jid);
		if (job == null)
			return null;
		JobData jd = new JobData("Job-"+jid, job);
		return getExtractedJobData(jd);		
	}
	
	public static JobExtractor getExtractedJobData(JobData jd) throws Exception {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(JobExtractor.serialize(jd));
			JobExtractor je = new JobExtractor();
			ObjectInputStream ois = new ObjectInputStream(bais);
			je.readSerialized(ois);
			return je;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot extract job", e);			
		}		
	}
	
	public static JobData getImportedJob(JobExtractor src) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		src.writeSerialized(oos);
		JobData jd = JobData.class.newInstance();
		JobExtractor.deserialize(jd, baos.toByteArray());
		return jd;
	}
	
	public static void setCurrentActivityOutput(long jid, String aName, XiNode reply) throws Exception {
		final Job job = EngineHelper.getJobPool().findJob(jid);
		job.getCurrentAttributes().setVariable(aName.replace(' ','-'), new Variable(reply));
		Object track = execute(job, "getCurrentTrack", new Object[0]);
		Object transition = getFieldByTypeName(track, null, "Transition");
		Object destination = execute(transition, "getDestination", new Object[0]);
		Object activityReplacement = Class.forName("com.tibco.plugin.timer.NullActivity").newInstance();
		Object context = getFieldByTypeName(destination, null, "ActivityContextImpl");
		Object outBindingRunner = execute(destination, "getOutputBinds", new Object[0]);
		setFieldByTypeName(outBindingRunner, "boolean", false);
		execute(activityReplacement, "init", new Object[] { context });
		setFieldByTypeName(destination, "Activity", activityReplacement);		
	}

	public static void resumePausedJob(long jid) {
		getJobPool().findJob(jid).resume();
	}
	
	public static void callProcessToActivityOutput(long jid, String path, String aName, XiNode input) throws Exception {
		final Job job = EngineHelper.getJobPool().findJob(jid);
		Class<?> callProcessClass = Class.forName("tdi.com.tibco.pe.core.MockedCallCustomProcessActivity");
		Object activityReplacement = callProcessClass.getDeclaredConstructors()[0].newInstance();
		setField(activityReplacement, "processName", path);
		setField(activityReplacement, "processRef", path);
		setField(activityReplacement, "wantsInputValidation", false);
		setField(activityReplacement, "wantsOutputValidation", false);				
		Object track = execute(job, "getCurrentTrack", new Object[0]);
		Object transition = getFieldByTypeName(track, null, "Transition");
		Object destination = execute(transition, "getDestination", new Object[0]);
		Object context = getFieldByTypeName(destination, null, "ActivityContextImpl");
		Object outBindingRunner = execute(destination, "getOutputBinds", new Object[0]);
		setFieldByTypeName(outBindingRunner, "boolean", false);
		execute(activityReplacement, "init", new Object[] { context });
		setFieldByTypeName(destination, "Activity", activityReplacement);			
	}

	public static void addLateJobListener(long jid) {
		getJobPool().findJob(jid).addJobListener(new JobEventStats(jid));		
	}	
	
	public static boolean setProperty(String key, String value) {
		Properties p = (Properties) getFieldByTypeName(null, Engine.class, "EngineProperties");
		if (p!=null) {
			p.setProperty(key, value);
			return true;
		}
		return false;
	}
	
	public static String getProperty(String key, String defValue) {
		Properties p = (Properties) getFieldByTypeName(null, Engine.class, "EngineProperties");
		return p!=null ? p.getProperty(key, defValue) : null;
	}
	
	public static String getSchedulerSummary() {
		long[] ids = EngineHelper.getJobPool().getJobIds();
		long active = 0;
		long dead = 0;
		long swapped = 0;
		long all = 0;
		if (ids!=null && ids.length > 0) {
			for (long id : ids) {
				Job job = EngineHelper.getJobPool().findJob(id);
				if (job.isActive())
					active++;
				if (job.isDead())
					dead++;
				if (job.isPaged())
					swapped++;
				all++;
			}			
		}
		return active+" ACTIVE, "+dead+" DEAD, "+swapped+" SWAPPED, "+all+" ALL";
	}

	@SuppressWarnings({"rawtypes","unchecked"})
	/**
	 * We operate also when engine is shutting down. To prevent NPE
	 * during these operations we inject dummy Workflow here
	 */
	public static void injectCanaryWorkflow() {
		final WorkflowLoader wl = EngineHelper.getJobPool().getWorkflowLoader();
		for (Field f : WorkflowLoader.class.getDeclaredFields()) {
			if (f.getType().getName().contains("HashMap")) {
				try {
					f.setAccessible(true);					
					HashMap chm = new HashMap() {						
						private static final long serialVersionUID = 1L;
						@Override
						public Collection values() {
							Collection values = super.values();
							LinkedList ll = new LinkedList(values);
							Collections.sort(ll, STRING_COMPARATOR);
							ConcurrentLinkedQueue clq = new ConcurrentLinkedQueue(ll);
							return clq;
						}
						
					};
					HashMap hm = (HashMap) f.get(wl);
					chm.put("", new WorkflowImpl("") {
						@Override
						public void uninit() {
							Logger.getInstance().debug("Canary uninit");							
						}});
					chm.putAll(hm);
					f.set(wl, chm);					
					break;
				}
				catch (Throwable t) {
					Logger.getInstance().debug("Cannot inject canary workflow");
				}
			}
		}		
	}	
}
