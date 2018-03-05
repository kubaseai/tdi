package tdi.core;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map.Entry;

import com.tibco.pe.core.Engine;
import com.tibco.pe.core.EngineHelper;
import com.tibco.pe.core.RepoAgentImpl;
import com.tibco.pe.core.WorkflowLoader;

public class ProcessHotUpdater {
	
	private static final String URI_PREFIX = "imp:///";
	
	public final static boolean reloadProcesses(HashMap<String,String> model) {
		try {
			for (Entry<String, String> en : model.entrySet()) {
				String path = en.getKey();
				if (path!=null && path.startsWith(URI_PREFIX))
					path = path.substring(URI_PREFIX.length());
				String content = en.getValue();
				FileOutputStream out = new FileOutputStream(path);
				out.write(content.getBytes());
				out.close();
			}
			RepoAgentImpl ra = (RepoAgentImpl) Engine.getRepoAgent();
			ra.clearCache();
			WorkflowLoader wl = new WorkflowLoader(ra, Engine.DEBUG_TRACER, Engine.DEBUG_TRACER,
				EngineHelper.getJobPool());
			wl.loadWorkflows();
			EngineHelper.getJobPool().setWorkflowLoader(wl);
			return true;
		}
		catch (Throwable e) {
			Logger.getInstance().debug("Cannot HotUpdate BW processes", e);	
			return false;
		}		
	}
}
