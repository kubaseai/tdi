package tdi.core;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.xml.sax.InputSource;

import com.tibco.pe.core.EngineHelper;
import com.tibco.pe.plugin.DataModel;
import com.tibco.xml.datamodel.XiNode;
import com.tibco.xml.datamodel.XiParserFactory;
import com.tibco.xml.xdata.bind.XPathRunner;
import com.tibco.xml.xdata.xpath.VariableList;

public class MockController {
	
	private final static ConcurrentHashMap<String,String> replacementRules = new ConcurrentHashMap<String,String>();
	private final static ConcurrentHashMap<String, Integer> replacements = new ConcurrentHashMap<String, Integer>();
	private final static String TRIGGER_ACTIVITY = "Activity";
	private final static String TRIGGER_CLASS = "Class";
	private final static String TRIGGER_PROCESS = "Process";
	
	public final static void addReplacementRule(String process, String activity, String xmlContentOrProcessPath) {
		if (process!=null && process.startsWith("/"))
			process = process.substring(1);		
		replacementRules.put(process+";"+activity, xmlContentOrProcessPath);
	}
	
	private final static String getReplacementForKey(String key, AtomicReference<String> keyRef) {
		String replacement = replacementRules.get(key);
		if (replacement!=null)
			keyRef.set(key);
		return replacement;
	}
	
	private final static String getTaskReplacement(String processName, String activityName, AtomicReference<String> keyRef) {
		if (processName!=null && processName.startsWith("/"))
			processName = processName.substring(1);
		
		String nestedProcessName = null;
		String nestedActivityName = null;
		int gtIdx = activityName.lastIndexOf('>');
		if (gtIdx >= 0) {
			int activityBeginIdx = activityName.lastIndexOf("/");
			if (activityBeginIdx > gtIdx+1) {
				nestedProcessName = activityName.substring(gtIdx+1, activityBeginIdx);
				nestedActivityName = activityName.substring(activityBeginIdx+1);
			}
		}
		
		String[] proc = { processName, nestedProcessName };
		String[] act = { activityName, nestedActivityName };
		String replacement = null;
		
		for (int i=0; i < proc.length; i++) {		
			int idx = (proc[i]+"").indexOf(".process/"); /*it's process + group */
			if (idx > 0) {
				proc[i] = proc[i].substring(0,  idx+8);				
			}		
			replacement = getReplacementForKey(proc[i]+";"+act[i], keyRef);
			if (replacement==null)
				replacement = getReplacementForKey("*;"+act[i], keyRef);	
			if (replacement==null) {
				for (Entry<String,String> en : replacementRules.entrySet()) {
					if ((proc[i]+";"+act[i]).matches(en.getKey().replace("*", ".*"))) {
						replacement = en.getValue();
						if (keyRef!=null)
							keyRef.set(en.getKey());
						break;
					}
				}
			}
			if (replacement!=null)
				break;
		}
		return replacement!=null && replacement.startsWith("/") ? replacement.substring(1) : replacement;
	}	
	
	public static boolean handleActivity(long jid, String processName,
			String activityName, XiNode activityData)
	{	
		for (String proc : replacementRules.values()) {
			if (proc.endsWith(".process") && activityName.contains(proc) || processName.contains(proc))
				return false;
		}
		String triggerType = TRIGGER_ACTIVITY;
		DataModel activity = EngineHelper.getActivity(jid, activityName);
		AtomicReference<String> keyRef = new AtomicReference<String>();
		String replacement = null;	
		boolean exactlyOnce = false;
		
		if (activity instanceof com.tibco.pe.core.CallProcessActivity) {
			try {
				com.tibco.pe.core.CallProcessActivity call = (com.tibco.pe.core.CallProcessActivity) activity;
				XPathRunner runner = (XPathRunner) EngineHelper.getFieldByName(call, XPathRunner.class, "runner");
				Field fProcessName = (Field) EngineHelper.getFieldByName(call, String.class, "processName", true);
				String callProc = (String) fProcessName.get(call);
				VariableList vars = (VariableList) EngineHelper.execute(EngineHelper.getJobPool().findJob(jid), "getCurrentAttributes", new Object[0]);
				if (runner != null) {
					callProc = runner.runString(vars); 
				}				
				replacement = getTaskReplacement(callProc, activityName, keyRef);				
				if (replacement!=null) {
					if (replacement.startsWith("!")) {
						replacement = replacement.substring(1);
						exactlyOnce = true;
					}
					fProcessName.set(call, replacement);
					triggerType = TRIGGER_PROCESS;
				}
			}
			catch (Throwable t) {}
		}		

		
		if (replacement==null)
			replacement = getTaskReplacement(processName, activityName, keyRef);
				
		if (replacement == null && activity!=null) {
			replacement = getTaskReplacement(processName, activity.getClassName(), keyRef);
			if (replacement!=null)
				triggerType = TRIGGER_CLASS;
		}
		
		if (replacement == null && activity!=null && activity.getClassName().equals("com.tibco.plugin.java.JavaMethodActivity")) {
			try {
				String javaClass = EngineHelper.getFieldByName(activity, activity.getClass(), "m_declaredClassName")+"";
				replacement = getTaskReplacement(processName, javaClass, keyRef);
				if (replacement!=null)
					triggerType = TRIGGER_CLASS;
			}
			catch (Throwable t) {}
		}
								
		if (replacement!=null) {
			if (replacement.startsWith("!")) {
				replacement = replacement.substring(1);
				exactlyOnce = true;				
			}
			if (replacements.put(processName+";"+activityName, Integer.valueOf(1))==null)
				Logger.getInstance().debug("[Mock"+triggerType+"] Running for '"+keyRef.get()+"', replacement="+replacement);
			if (keyRef.get()!=null && exactlyOnce)
				replacementRules.remove(keyRef.get());
			
			try {
				if (replacement.startsWith("<?xml")) {
	     			XiNode xi = XiParserFactory.newInstance().parse(new InputSource(new StringReader(replacement)));
	     			EngineHelper.setCurrentActivityOutput(jid, activityName, xi);
					return true;
				}
				else if (triggerType == TRIGGER_PROCESS) {
					return true;
				}
				else if (replacement.endsWith(".process")) {
					EngineHelper.callProcessToActivityOutput(jid, replacement, activityName, activityData);
					return true;
				}
			}
			catch (Throwable e) {
				Logger.getInstance().debug("Cannot mock call", e);
				throw new RuntimeException("Cannot call mock", e);
			}
		}
		return false;
	}	
	
	public final static void main(String[] args) {
		MockController.addReplacementRule("/Business Processes/Vetro/Trade Subscriber/Send GMI Trade Message.process", "Get MQ Sender", "/Unit Tests/MockedSend.process");
		System.out.println(MockController.getTaskReplacement("/Business Processes/Vetro/Trade Subscriber/Send GMI Trade Message.process", "Get MQ Sender", null));
	}
}
