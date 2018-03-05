/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.core;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import tdi.core.activities.Analyzer;
import tdi.core.activities.MarkerDefinition;
import tdi.transport.EventType;

import com.tibco.pe.core.Engine;
import com.tibco.pe.core.EngineHelper;
import com.tibco.pe.core.JobEvent;
import com.tibco.pe.core.JobListener;
import com.tibco.pe.core.ProcessStarter;
import com.tibco.pe.plugin.DataModel;
import com.tibco.xml.datamodel.XiNode;
import com.tibco.xml.datamodel.helpers.XiSerializer;
import com.tibco.xml.xdata.xpath.Variable;
import com.tibco.xml.xdata.xpath.VariableList;

public class JobEventStats implements JobListener, Serializable {
	
	public static final long serialVersionUID = 20120112220000L;
	@RelevantField(name="job") private long jobId = -1;
	@RelevantField(name="proc") private String processName = "";
	private transient JobEvent lastEvent = null;
	private transient XiNode lastError = null;
	@RelevantField(name="cr") private int created = 0;
	@RelevantField(name="fn") private int completed = 0;
	@RelevantField(name="rn") private int running = 0;
	@RelevantField(name="fl") private int flowLimit = 0;
	@RelevantField(name="tc") private int threadCount = 0;
	@RelevantField(name="st") private long start = 0;
	private Long delayed = null;
	@RelevantField(name="en") private long end = 0;
	@RelevantField(name="ec") private long errorCount = 0;
	@RelevantField(name="wt") private long suspendTime = 0;
	private long lastSuspend = 0;
	private long evalTime = 0;
	@RelevantField(name="et") private long et = 0;
	private long tid = 0;
	
	private LinkedList<Long> tsw = new LinkedList<Long>();
	private LinkedList<String> errors = new LinkedList<String>();
	private HashMap<String, String> metrics = new HashMap<String, String>();
	@RelevantField(name="t") private String type = EventType.STATS;
	
	@ConfigParam(name="PROFILER", desc="Record activities durations", value="yes|no")
	private transient HashMap<String,Long> _profiler = null;
	private transient String profilerResultsCached = null;
	private String lastMarkerInMessage = null;
	private String lastMakerOutMessage = null;
	private LinkedList<StepEntry> profilerResults = new LinkedList<StepEntry>();
	private String markersDefsRaw = null;
	private HashMap<String,LinkedHashSet<String>> markersMap = new HashMap<String, LinkedHashSet<String>>();
	private LinkedHashSet<String> markersMsgsSet = new LinkedHashSet<String>();
	private StringBuilder markersMsgs = new StringBuilder(65535);
	private TreeMap<Integer,LinkedHashSet<String>> globalMarkers = new TreeMap<Integer, LinkedHashSet<String>>();
	
	@ConfigParam(name="LOGGER", desc="Extract data logged with WriteToLog/Log4j", value="yes|no")
	private ValueHolder<Boolean> interceptLogger = PEProc.getVolatileBooleanProperty("LOGGER", "NO");
	
	@ConfigParam(name="IGNORE_ERROR_TEXT", desc="Do not treat as real errors errors with given text",
	value="TDI_IGNORE_ERROR|string")
	private ValueHolder<String> ignoreErrorWithText = PEProc.getVolatileStringProperty("IGNORE_ERROR_TEXT", "ForceError|Force Error|TriggerError|Logger");
	
	@ConfigParam(name="MAX_MKM_PAYLOAD", desc="Truncate extracted marker value to given length",
	value="262144|integer")
	private static ValueHolder<Long> MAX_MKM_PAYLOAD = PEProc.getVolatileLongProperty("MAX_MKM_PAYLOAD", "262144");
	private final static String WRITE_TO_LOG = "com.tibco.pe.core.WriteToLogActivity";
	private final static String JAVA_ACTIVITY = "com.tibco.plugin.java.JavaActivity";
	
	private final static boolean DELAYED_INIT = !isInsideBW();
	
	protected final static int JOB_SUSPENDED = 0, JOB_ACTIVE = 1, JOB_STANDBY = 2, JOB_STOPPING = 3;
	private final static int MAX_INDEXED_MARKER_LEN = 2000;
	private static volatile String cachedRepoName = null;
	
	private HashMap<String,Long> getProfiler() {
		if (_profiler==null)
			_profiler = PEProc.getVolatileBooleanProperty("PROFILER", "NO").get() ? new HashMap<String, Long>() : null;
		return _profiler;
	}
	
	@RelevantField(name="repo") 
	public final static String getRepositoryName() {
		if (cachedRepoName!=null)
			return cachedRepoName;
		String rn = null;
		int cnt = 0;
		while (rn==null) {	
			try {
				rn = Engine.getRepoAgent().getProjectName();
			}
			catch (Exception re) {
				rn = EngineHelper.getProperty("name", "<empty>");
			}		
			if (rn!=null && rn.endsWith("_root"))
				rn = rn.substring(0, rn.length()-5);
			if (rn==null) { /* Engine is starting */
				try {
					Thread.sleep(1000);
					if (++cnt > 3)
						rn = "DEFAULT_PROJECT";
				}
				catch (InterruptedException e) {}
			}
		}		
		return (cachedRepoName=rn);
	}
	
	@RelevantField(name="marker_defs")
	public String getMarkersDefsRaw() {
		return markersDefsRaw;
	}
	
	@RelevantField(name="mk")
	public StringBuilder getMarkers() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, LinkedHashSet<String>> en : markersMap.entrySet()) {
			HashSet<String> values = en.getValue();
			for (String val : values) {
				sb.append(en.getKey()).append("=").append(val).append("|");
			}
		}
		return sb;
	}
	
	@RelevantField(name="markers_msgs")
	public StringBuilder getMarkersMessages() {
		return markersMsgs;
	}
	
	public final static class StepEntry implements Serializable {
		private static final long serialVersionUID = 1L;
		public long t0 = 0;
		public long t1 = 0;
		public long t2 = 0;
		public long t3 = 0;
		public String activity = null;
		public String kind = null;
		public String input;
		public String output;
		
		private boolean isNonStandard() {
			return kind!=null;
		}
		
		public StepEntry(long t0, long t1, long t2, String name) {
			this.t0 = t0;
			this.t1 = t1;
			this.t2 = t2;
			this.activity = name;
		}
		
		public StepEntry(String s, String kind) {
			this.output = s;
			this.kind = kind;
		}
		
		public String getDescription() {
			return activity;
		}

		public String toString() {
			if (isNonStandard())
				return this.output;
			long dt = t3-t1;
			if (dt<0)
				dt = 0;
			return activity+" :: "+(t2-t1)+"ms / "+dt+"ms at "+(t1-t0)+"ms";
		}
		
		public boolean isRelevant() {
			return isNonStandard() || t3-t1>0;
		}
		public long[] getTimes() {
			if (kind!=null)
				return new long[0];
			long dt = t3-t1;
			return new long[] {
				t1-t0, t2-t1, dt < 0 ? 0 : dt 
			};
		}
	}
		
	public HashMap<String,Object> getFields() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		for (Field f : getClass().getDeclaredFields()) {
			RelevantField rf = f.getAnnotation(RelevantField.class);
			if (rf!=null) {
				try {
					f.setAccessible(true);
					map.put(rf.name(), f.get(this));					
				}
				catch (Exception e) {}
			}
		}
		for (Method m : getClass().getDeclaredMethods()) {
			RelevantField rf = m.getAnnotation(RelevantField.class);
			if (rf!=null) {
				try {
					m.setAccessible(true);					
					map.put(rf.name(), m.invoke(this, new Object[] {}));					
				}
				catch (Exception e) {}
			}
		}		
		return map;
	}
			
	private static boolean isInsideBW() {
		try {
			Class.forName("COM.TIBCO.hawk.ami.AmiMethodInterface");
			return true;
		}
		catch (Exception e) {}
		return false;
	}

	public JobEventStats(long jid) {
		start = System.currentTimeMillis();
		jobId = jid;
		if (!DELAYED_INIT)
			init();		
	}
	
	private void init() {
		/*repoName = getRepositoryName();*/
		ProcessStarter ps = EngineHelper.getProcessStarter(jobId);
		if (ps!=null) {
			created = ps.getCreated();
			completed = ps.getCompleted();
			running = ps.getJobsRunning();
			flowLimit = ps.getFlowLimit();
		}
		threadCount = EngineHelper.getJobPool().getThreadCount();
	}
	
	private String getLastActivityName(boolean inXPath) {
		String an = inXPath ? lastEvent.activityName.replace(' ', '-') : lastEvent.activityName;
		int p = an.lastIndexOf('/');
		if (p!=-1)
			an = an.substring(p+1);
		return an;
	}
	
	private XiNode getActivityData() {
		VariableList vl = EngineHelper.getJobData(jobId);
		if (vl!=null) {
			String an = getLastActivityName(true);
			Variable v = vl.getVariable(an);
			if (v!=null)
				return v.getValue();
		}
		return null;
	}
	
	private XiNode getActivityError() {
		VariableList vl = EngineHelper.getJobData(jobId);
		if (vl!=null) {
			Variable v = vl.getVariable("_error");
			if (v!=null)
				return v.getValue();
		}
		return null;
	}	
	
	protected List<String> getJobVariablesNames() {
		LinkedList<String> names = new LinkedList<String>();
		VariableList vl = EngineHelper.getJobData(jobId);
		if (vl!=null) {
			for (Object o : vl.getVariableNames())
				names.add(o.toString());
		}
		return names;
	}
	
	public String nm() {
		return lastEvent.processName+" :: "+getLastActivityName(false);
	}

	private String getActivityClassName(JobEvent je) {
		if (je==null)
			return "";
		DataModel av = EngineHelper.getActivity(jobId, je.activityName);
		return av!=null ? av.getClassName() : "";
	}
	
	public String getLastActivityClassName() {
		return lastEvent!=null ? getActivityClassName(lastEvent) : "";
	}
	
	public boolean afterExecution(JobEvent je) {
		long t2 = System.currentTimeMillis();
		StepEntry se = null;
		if (delayed == null)
			delayed = t2 - start;
		if (getProfiler()!=null) {			
			Long t1 = _profiler.remove(je.activityName);
			if (t1!=null) {
				profilerResults.add(se = new StepEntry(start, t1, t2, je.activityName));
				String domProfiling = XmlContentTracerCompanion.getDomProfiling();
				if (domProfiling!=null && domProfiling.length()>0)
					profilerResults.add(new StepEntry("XML tree times: "+domProfiling, "dom"));
			}
			else
				se = new StepEntry(start, start, t2, je.activityName);
				
		}
		
		if (lastEvent!=null && lastEvent.activityData!=null && je.activityData==null)
			je.activityData = lastEvent.activityData;
		lastEvent = je;
						
		XiNode data = getActivityData();
		DataModel av = EngineHelper.getActivity(jobId, je.activityName);	
		XiNode config = av!=null ? av.getConfigParms() : null;
		XiNode input = je.activityData;
		Analyzer.analyzeProcess(this, av!=null ? av.getClassName() : "", data, config, input);
		if (se!=null) {
			se.input = lastMarkerInMessage;
			se.output = lastMakerOutMessage;
		}
		
		if (getProfiler()!=null) {
			evalTime = System.currentTimeMillis();
			if (tid!=Thread.currentThread().getId()) {
				tid = Thread.currentThread().getId();
				tsw.add(evalTime-start);
			}
		}
		return true;
	}

	public boolean beforeExecution(JobEvent je) {
		if (MockController.handleActivity(jobId, je.processName, je.activityName, je.activityData)) {
			return true;
		}
		long t3 = System.currentTimeMillis();
		if (DELAYED_INIT)
			init();
		lastEvent = je;		
		processName = lastEvent.processName;
		if (getProfiler()!=null) {
			_profiler.put(lastEvent.activityName, System.currentTimeMillis());
			if (tid!=Thread.currentThread().getId()) {
				if (tid>0)
					tsw.add(t3-start);
				tid = Thread.currentThread().getId();
			}
			if (!profilerResults.isEmpty()) {
				et += (t3 - profilerResults.getLast().t2);
				profilerResults.getLast().t3 = t3;
			}			
		}
		if (interceptLogger.get()) {
			String a = getActivityClassName(je);
			if ((WRITE_TO_LOG.equals(a) || (JAVA_ACTIVITY.equals(a) && getActivityName().toLowerCase().contains("log4j")) ) 
				&& je!=null && je.activityData!=null)
			{
				putMarkerMessage(getActivityNameAsXmlComment(), XiSerializer.serialize(je.activityData));
				try {
					EngineHelper.setCurrentActivityOutput(jobId, je.activityName, je.activityData);
				} 
				catch (Exception e) {
					Logger.getInstance().debug("Cannot swallow logging activity", e);
				}
			}
		}
		return true;
	}

	public void errorLogged(String process, String activity, XiNode xinode, long jid) {
		XiNode er = getActivityError();
		Analyzer.analyzeError(this, er!=null ? er : xinode);
	}

	public void processCalled(JobEvent je, String processName, boolean spawned) {
		lastEvent = je;
	}

	public void stateChanged(boolean active, long l) {		
		if (!active && lastSuspend==0)
			lastSuspend = System.currentTimeMillis();
		else if (active && lastSuspend > 0) {
			suspendTime += System.currentTimeMillis() - lastSuspend;
			lastSuspend = 0;
		}
	}

	public void trackAborted(String activity, long jid, int i) {}

	public void transitionEvaluated(String process, String activity, String name, boolean result, long jid, int i) {
		long t = System.currentTimeMillis();
		XiNode error = getActivityError();
		if (error!=null && error!=lastError) {
			incErrorCount(error);
			lastError = error;
		}
		if (getProfiler()!=null) {
			if (!profilerResults.isEmpty()) {
				profilerResults.getLast().t3 = t;
			}			
		}
	}		

	public boolean wantsActivityInput() {
		return end == 0;
	}

	public void jobRemoved() {
		end = System.currentTimeMillis();
		lastEvent = null;		
		if (getProfiler()!=null) {
			if (!tsw.isEmpty()) {
				StepEntry se = new StepEntry("Thread-switches("+tsw.size()+") :: "+tsw, "tsw");
				se.t1 = tsw.size();
				profilerResults.add(se);
			}
		}
	}

	public long getJobId() {
		return jobId;
	}	

	public String getRepoName() {
		return getRepositoryName();
	}
	
	public JobEvent getLastEvent() {
		return lastEvent;
	}	

	public int getCreated() {
		return created;
	}
	
	public int getCompleted() {
		return completed;
	}	

	public int getRunning() {
		return running;
	}	

	public long getStart() {
		return start;
	}	

	@RelevantField(name="d")
	public long getDelayed() {
		return delayed != null ? delayed : 0;
	}

	public String getActivityName() {
		return lastEvent!=null ? lastEvent.activityName : null;
	}

	public String getProcessName() {
		return processName;
	}

	public long getEnd() {
		return end;
	}

	public HashMap<String, String> getMetrics() {
		return metrics;
	}

	public int getFlowLimit() {
		return flowLimit;
	}
	
	public void setType(String t) {
		this.type = t;		
	}
	
	public JobEventStats setTypeAndReturn(String t) {
		this.type = t;
		return this;
	}
	
	public long getSuspendTime() {
		return suspendTime;
	}	

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EVT [");
		for (Entry<String,Object> en : getFields().entrySet()) {
			builder.append(en.getKey()).append('=').append(en.getValue()+"").append(", ");
		}
		builder.append("]");
		return builder.toString();
	}

	public long getRunningDuration() {
		return end==0 ? 0 : System.currentTimeMillis()-start;
	}

	public int getThreadCount() {
		return threadCount;
	}

	public long incErrorCount(XiNode er) {
		if (er!=null) {
			String xmlEr = XiSerializer.serialize(er).toLowerCase();
			boolean ignoreError = false;
			for (String errorToken : ignoreErrorWithText.get().split("\\|")) {
				if (xmlEr.contains(errorToken.toLowerCase())) {
					ignoreError = true;
					break;
				}				
			}
			if (!ignoreError)
				errors.add( xmlEr );
		}
		return ++errorCount;		
	}
	
	public void addErrorMsg(String msg) {
		errors.add(msg);
		++errorCount;		
	}

	public long getErrorCount() {
		return errorCount;
	}

	public String getEventType() {
		return type;
	}
	
	public long getEvalTime() {
		return et;
	}

	@RelevantField(name="mt")
	public String getMetricsString() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String,String> en : metrics.entrySet()) {
			sb.append(en.getKey()).append("=").append(en.getValue()).append(",");
		}
		return sb.length()>0 ? sb.toString() : null;
	}

	@RelevantField(name="em")
	public String getErrorMessages() {
		return errors.size()==0 ? null : errors.toString();
	}
	
	@RelevantField(name="profiler")
	public String getProfilerResults() {
		if (profilerResultsCached!=null)
			return profilerResultsCached;
		if (profilerResults.size()==0)
			return null;
		StringBuilder sb = new StringBuilder(); 
		for (StepEntry pe : profilerResults) {
			if (pe.isRelevant())
				sb.append(pe).append(", \r\n");
		}
		return profilerResultsCached = sb.toString();
	}
	
	public final static void main2(String[] args) {
		StringBuilder hb = new StringBuilder("create 'events', ");
		JobEventStats je = new JobEventStats(0);
		for (Map.Entry<String, Object> en : je.getFields().entrySet()) {
			hb.append("'").append(en.getKey()).append("',");
		}
		hb.append("'JMSTimestamp', 'JMSExpiration'");
		Logger.getInstance().debug("HBase table definition: ");
		Logger.getInstance().debug(hb.toString());
	}

	public void setIncludeMarkersDefs(boolean b) {
		markersDefsRaw = b ? MarkerDefinition.getRawString() : null;		
	}

	public String[] getGlobalMarkers() {
		if (globalMarkers.size()==0)
			return new String[0];
		String tab[] = new String[Math.min(globalMarkers.lastKey(), 256)];
		for (int i=0; i < tab.length; i++) {
			LinkedHashSet<String> hs = globalMarkers.get(i+1);
			int size = hs!=null ? hs.size() : 0;
			if (size >= 1) {
				DataNormalizer.normalizeSet(hs);
				Iterator<String> it = hs.iterator();
				if (it.hasNext()) {
					StringBuilder sb = new StringBuilder();
					do {
						String next = it.next();
						if (sb.length()+next.length() > MAX_INDEXED_MARKER_LEN) {
							Logger.getInstance().debug("[ERROR] Badly defined marker "+(i+1)+
								" will be truncated to limit "+MAX_INDEXED_MARKER_LEN+" characters");
							break;
						}
						sb.append(next);
						if (it.hasNext())
							sb.append("|");
					}
					while (it.hasNext());
					tab[i] = sb.toString();
				}
			}
		}
		return tab;
	}

	public void addGlobalMarker(int mk, String value) {
		LinkedHashSet<String> hs = globalMarkers.get(mk);
		if (hs==null)
			globalMarkers.put(mk, hs = new LinkedHashSet<String>());
		if (value!=null && !hs.contains(value)) // do not overwrite
			hs.add(value);
	}

	public void putMarker(String markerName, String value) {
		LinkedHashSet<String> list = markersMap.get(markerName);
		if (list==null) 
			markersMap.put(markerName, list = new LinkedHashSet<String>());
		list.add(value);	
	}

	public String getActivityNameAsXmlComment() {
		String a = getActivityName();
		String hint = a!=null ?
			"\r\n<!-- " + a.replace("<", "&lt;").replace(">", "&gt;") + " -->\r\n" : "\r\n";
		return hint;
	}
	
	public boolean putMarkerMessage(String hint, String msg) {
		long lenLimit = MAX_MKM_PAYLOAD.get();
		if (markersMsgs.length() > lenLimit) {
			Logger.getInstance().debug("[Error] Overflow in job event's marker message buffer: "+markersMsgs.length()+" vs "+lenLimit);
			msg = msg.substring(0, Math.min((int)(lenLimit - 11), msg.length())) + "<TRUNCATED>";
		}
		boolean added = markersMsgsSet.add(msg);
		if (added) {
			markersMsgs.append(hint).append(msg).append("|");
		}
		else {
			if (hint.trim().length()>0)
				markersMsgs.append(hint).append("|");
		}	
		return added;
	}
	
	public boolean hasOomError() {
		return errors.size() > 0 && errors.getLast()!=null &&
			errors.getLast().contains("java.lang.OutOfMemoryError");
	}

	public void clear() {
		lastEvent = null;
		lastError = null;
		if (tsw!=null)
			tsw.clear();
		tsw = null;
		if (errors!=null)
			errors.clear();
		errors = null;
		if (metrics!=null)
			metrics.clear();
		metrics = null;
		if (getProfiler()!=null)
			_profiler.clear();
		profilerResultsCached = null;
		if (profilerResults!=null)
			profilerResults.clear();
		profilerResults = null;
		if (markersMap!=null) {
			for (LinkedHashSet<String> hs : markersMap.values())
				hs.clear();
			markersMap.clear();
			markersMap = null;
		}
		if (markersMsgsSet!=null)
			markersMsgsSet.clear();
		markersMsgsSet = null;
		markersMsgs = null;
		if (globalMarkers!=null) {
			for (LinkedHashSet<String> hs : globalMarkers.values())
				hs.clear();
			globalMarkers.clear();
		}
		globalMarkers = null;
		lastMarkerInMessage = null;
		lastMakerOutMessage = null;
	}

	public LinkedList<StepEntry> getSteps() {
		return profilerResults;
	}

	public void setLastMarkerMessage(String msg, int kind) {
		if (kind == 0)
			lastMarkerInMessage = msg;
		else
			lastMakerOutMessage = msg;		
	}
	
	protected JobEventStats cloneLite() {
		JobEventStats je = new JobEventStats(this.jobId);
		je.processName = this.processName;
		je.created = this.created;
		je.completed = this.completed;
		je.running = this.running;
		je.flowLimit = this.flowLimit;
		je.threadCount = this.threadCount;
		je.start = this.start;
		je.delayed = this.delayed;
		je.end = this.end;
		je.errorCount = this.errorCount;
		je.suspendTime = this.suspendTime;
		je.lastSuspend = this.lastSuspend;
		je.evalTime = this.evalTime;
		je.et = this.et;
		je.errors.addAll(this.errors);
		je.metrics.putAll(this.metrics);
		je.type = this.type;
		je.profilerResults.addAll(this.profilerResults);		
		return je;
	}

	public static JobEventStats createPushPopEvent(long id, String type, String subtype, String key, String content) {
		JobEventStats evt = new JobEventStats(id);
		if (type==null)
			type = subtype;
		evt.setType(type);
		PushPopEventAdapter ea = evt.toPushPopEventAdapter();
		ea.setSubtype(subtype);
		ea.setKey(key);
		ea.setContent(content);
		return evt;
	}	
	
	public class PushPopEventAdapter implements Serializable {
		private static final long serialVersionUID = 1L;
		private JobEventStats evt = null;
		
		public PushPopEventAdapter(JobEventStats je) {
			evt = je;
		}
		public void setSubtype(String type) {
			evt.metrics.put("type", type);
		}
		public void setKey(String key) {
			evt.metrics.put("key", key);
		}
		public void setEntry(String key, String value) {
			evt.metrics.put(key, value);
		}
		public void setContent(String content) {
			evt.markersMsgs.append(content);
		}
		public String getSubtype() {
			return evt.metrics.get("type");
		}
		public String getKey() {
			return evt.metrics.get("key");
		}
		public String getContent() {
			return evt.markersMsgs.toString();
		}
		public void getEntry(String key) {
			evt.metrics.get(key);
		}
	}
	
	public PushPopEventAdapter toPushPopEventAdapter() {
		return new PushPopEventAdapter(this);
	}
	
	public final static void main(String[] args) {
		PEProc.setRuntimeProperty("TSI_TRANSPORT", "tdi.transport.VoidTransport");
		cachedRepoName = "PROJECT";
		JobEventStats jes = new JobEventStats(1000);
		System.out.println(jes.toString());
	}
}
