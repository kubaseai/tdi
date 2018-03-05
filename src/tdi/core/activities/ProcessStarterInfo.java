package tdi.core.activities;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;

import com.tibco.pe.core.EngineHelper;
import com.tibco.pe.core.JobCreator;

public class ProcessStarterInfo {
	
	private String name = null;
	private String activity = null;
	private static HashMap<String, ProcessStarterInfo> map = new HashMap<String, ProcessStarterInfo>();
	static {
		for (ProcessStarterInfo psi : list()) {
			map.put(psi.name+";"+psi.activity, psi);
		}
	}
	
	public ProcessStarterInfo(String name, String activity) {
		this.name = name;
		this.activity = activity;
	}

	private static LinkedList<ProcessStarterInfo> list() {
		LinkedList<ProcessStarterInfo> list = new LinkedList<ProcessStarterInfo>();
		Enumeration<Object> en = EngineHelper.getJobPool().getProcessStarters();
		while (en.hasMoreElements()) {
			Object psOrAgent = en.nextElement();
			if (psOrAgent instanceof com.tibco.pe.core.ProcessStarter) {
				com.tibco.pe.core.ProcessStarter ps = (com.tibco.pe.core.ProcessStarter)psOrAgent;
				list.add(new ProcessStarterInfo(ps.getName(), ps.getStarterActivityName()));
			}
			else if (psOrAgent instanceof JobCreator) {
				JobCreator jc = (JobCreator)psOrAgent;
				list.add(new ProcessStarterInfo(jc.getName(), jc.getStarterActivityName()));
			}
		}		
		return list;
	}
	
	public static boolean isStarter(String name, String activity) {
		return map.get(name+";"+activity)!=null;
	}
}
