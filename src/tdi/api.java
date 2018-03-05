package tdi;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.xml.sax.InputSource;

import com.tibco.xml.datamodel.XiNode;
import com.tibco.xml.datamodel.XiParserFactory;
import com.tibco.xml.datamodel.helpers.XiSerializer;
import com.tibco.xml.xdata.xpath.Variable;
import com.tibco.xml.xdata.xpath.VariableList;

import tdi.core.ConfigParam;
import tdi.core.EngineHelperApi;
import tdi.core.JobEventStats;
import tdi.core.MockController;
import tdi.core.PEProc;
import tdi.core.ProcessHotUpdater;
import tdi.core.ProtectingClassLoader;
import tdi.core.XiNodeWrapper;

public class api extends apiHome {
	
	private static String jarName = null;
	private static LinkedList<String> paramsInfo = null;
	public final static api INSTANCE = new api();
	
	@Override
	public void HibernateProcess(long jid) throws Exception {
		String xml = EngineHelperApi.hibernateJobToXml(jid);
		JobEventStats pushEvent = JobEventStats.createPushPopEvent(jid, "push", "hib", "process", xml);
		PEProc.addEvent(pushEvent);
	}
		
	@Override
	public void RestoreProcess(String xml) throws Exception {
		EngineHelperApi.resumeHibernatedJobFromXml(xml);		
	}
	
	@Override
	public void UpdateProcessDefinition(HashMap<String,String> definition) {
		ProcessHotUpdater.reloadProcesses(definition);
	}	
	
	@Override
	public String GetJobVariableAsXml(long jid, String varName) {
		XiNode node = null;
		VariableList vl = EngineHelperApi.getJobData(jid);
		if (vl!=null) {
			Variable v = vl.getVariable(varName);
			if (v!=null)
				node = v.getValue();
		}
		else if ("_globalVariables".equals(varName)) {
			node = com.tibco.pe.core.Engine.getDeployedVarsVariable("DEFAULT_PROJECT").getValue();
		}
		try {
			if (node!=null)
				return XiNodeWrapper.nodeToString(new XiNodeWrapper(node));
			return "<not found>";
		}
		catch (Exception e) {
			return "<exception: "+e+">";
		}
	}	
	
	@Override
	public String ReplaceGlobalVariables(String vars) {
		try {
			Variable v = com.tibco.pe.core.Engine.getDeployedVarsVariable("DEFAULT_PROJECT");
			String previous = XiSerializer.serialize(v.getValue());
			XiNode gvNode = XiParserFactory.newInstance().parse(new InputSource( new StringReader(vars) ));
			gvNode.getFirstChild().setNamespace("", "http://www.tibco.com/pe/DeployedVarsType");
			java.lang.reflect.Field f = v.getClass().getDeclaredField("mValue");
			f.setAccessible(true);
			f.set(v, gvNode);
			return previous;
		}
		catch (Exception exc) {
			throw new RuntimeException("Failed to set GV", exc);
		}
	}	
	
	@Override
	public void AddMockingRule(String process, String activity, String xmlOrProcessReplacement) {
		MockController.addReplacementRule(process, activity, xmlOrProcessReplacement);
	}	
	
	@Override
	public String GetRepoPath() {
		return com.tibco.pe.core.Engine.getRepoAgent().getAbsoluteURIFromProjectRelativeURI("/");		
	}	
	
	@Override
	public String SetJobVariableFromXml(long jid, String varName, String xml) {
		VariableList vl = EngineHelperApi.getJobData(jid);
		try {
			if (vl!=null) {
				vl.setVariable(varName, new Variable(XiParserFactory.newInstance().parse(
					new InputSource(new StringReader(xml)))));
				return "";
			}
			return "<error: job invalid>";
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to set variable", e);
		}
	}
	
	private final static void filterAnnotations(Annotation[] in, List<String> out) {
		for (Annotation an : in) {
			if (an.annotationType().getName().equals(ConfigParam.class.getName())) {
				out.add(an.toString().replace("@tdi.core.ConfigParam", ""));							
			}
		}
	}
	
	private static String getJarName() {
		if (jarName==null) {
			jarName = ProtectingClassLoader.getJarName();
		}
		return jarName;
	}	
	
	@Override
	public List<String> GetConfigParameters() {
		if (paramsInfo!=null)
			return paramsInfo;
		String nakedJarPath = getJarName();
		LinkedList<String> list = new LinkedList<String>();
		JarFile jar = null;
		ProtectingClassLoader pcl = null;
		try {
			File jarFileLocation = new File(nakedJarPath);
			jar = new JarFile(jarFileLocation);			
			Enumeration<JarEntry> en = jar.entries();
			
			while (en.hasMoreElements()) {
				JarEntry je = en.nextElement();
				int p = je.getName().lastIndexOf(".class");
				if (p!=-1 && je.getName().startsWith("tdi")) {
					ClassLoader cl = Thread.currentThread().getContextClassLoader();
					try {
						pcl = new ProtectingClassLoader(new URL("jar:file://"+jarFileLocation+"!/"));
						Class<?> clazz = pcl.loadClass(je.getName().substring(0, p).replace("/", "."));
						filterAnnotations(clazz.getAnnotations(), list);
						try {
							for (Field f : clazz.getDeclaredFields()) {
								try {
									filterAnnotations(f.getAnnotations(), list);
								}
								catch (Throwable e2) {}								
							}
						}
						catch (Throwable e1) {}
						try {
							for (Method m: clazz.getDeclaredMethods()) {
								try {
									filterAnnotations(m.getAnnotations(), list);
								}
								catch (Throwable e2) {}	
							}
						}
						catch (Throwable e1) {}
					}
					catch (Throwable e) {}
					Thread.currentThread().setContextClassLoader(cl);
				}
			}
		}
		catch (Exception e) {}
		finally {
			if (jar!=null) {
				try {
					jar.close();
				}
				catch (IOException e) {}
			}
			if (pcl!=null) {
				try {
					pcl.close();
				}
				catch (IOException e) {}
			}
		}
		
		Collections.sort(list);		
		return paramsInfo = list;
	}	
	
	@Override
	public List<String> GetProcessesFromDir(String dir) {
		LinkedList<String> list = new LinkedList<String>();
		String repoPath = EngineHelperApi.getRepoAgent().getAbsoluteURIFromProjectRelativeURI("/");
		LinkedList<File> todo = new LinkedList<File>();
		todo.add(new File(repoPath+"/"+dir));
		while (todo.size() > 0) {
			LinkedList<File> newRun = new LinkedList<File>();
			for (File f : todo) {
				if (f.getName().endsWith(".process")) {
					list.add(f.getAbsolutePath().substring(repoPath.length()));
				}
				else if (f.isDirectory()) {
					for (File entry : f.listFiles()) {
						newRun.add(entry);
					}
				}
			}
			todo = newRun;
		}
		return list;
	}
	
	@Override
	public void instrument(Connection conn) {
		PEProc.instrumentLate(null);
		apiHome.INSTANCE = this;		
	}
	
	@Override
	public void setRuntimeProperty(String key, String v) {
		PEProc.setRuntimeProperty(key, v);
	}
	
	public final static void main(String[] args) {
		api.jarName = "C:\\tibco\\bw\\5.11\\hotfix\\lib\\tdi.jar";
		for (String s : new api().GetConfigParameters()) {
			System.out.println(s);
		}
	}
}
