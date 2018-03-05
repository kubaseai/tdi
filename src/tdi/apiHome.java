package tdi;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;

public class apiHome {	
	
	protected static apiHome INSTANCE = null;	
	
	public static synchronized apiHome INSTANCE() {
		if (INSTANCE==null) {
			INSTANCE = new api();		
		}
		return INSTANCE;
	}
	
	public void HibernateProcess(long jid) throws Exception {
		INSTANCE().HibernateProcess(jid);
	}
	
	public void RestoreProcess(String xml) throws Exception {
		INSTANCE().RestoreProcess(xml);
	}
	
	public void UpdateProcessDefinition(HashMap<String,String> definition) {
		INSTANCE().UpdateProcessDefinition(definition);
	}
	
	public String GetJobVariableAsXml(long jid, String varName) {
		return INSTANCE().GetJobVariableAsXml(jid, varName);
	}
	
	public String ReplaceGlobalVariables(String vars) {
		return INSTANCE().ReplaceGlobalVariables(vars);
	}
	
	public void AddMockingRule(String process, String activity, String xmlOrProcessReplacement) {
		INSTANCE().AddMockingRule(process, activity, xmlOrProcessReplacement);
	}
	
	public String GetRepoPath() {
		return INSTANCE().GetRepoPath();
	}
	
	public String SetJobVariableFromXml(long jid, String varName, String xml) {
		return INSTANCE().SetJobVariableFromXml(jid, varName, xml);
	}
	
	public List<String> GetConfigParameters() {
		return INSTANCE().GetConfigParameters();
	}
	
	public List<String> GetProcessesFromDir(String dir) {
		return INSTANCE().GetProcessesFromDir(dir);
	}
	
	public void instrument(Connection conn) {
		INSTANCE().instrument(conn);
	}

	public void setRuntimeProperty(String key, String v) {
		INSTANCE().setRuntimeProperty(key, v);
	}
}
