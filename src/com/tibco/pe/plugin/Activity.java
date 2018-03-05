package com.tibco.pe.plugin;

import java.io.Serializable;
import java.util.List;

import org.xml.sax.SAXException;

import tdi.core.CompileTimeStub;

import com.tibco.bw.store.RepoAgent;
import com.tibco.xml.datamodel.XiNode;
import com.tibco.xml.schema.SmElement;
import com.tibco.xml.schema.SmParticleTerm;
import com.tibco.xml.xdata.InputData;

@CompileTimeStub
@SuppressWarnings("serial")
public abstract class Activity extends Component implements DataModel {
	
	protected XiNode configParms;
	protected ActivityContext context;
	protected EngineContext engine;
	protected RepoAgent repoAgent;
	protected ProcessDefinitionContext procDef;
	private String uri;
	public static final String ELAPSED_TIME_PROPERTY = "bw.activity.output.stats.elapsedTime";
	public static final String ELAPSED_TIME_NAME = "ElapsedTimeinMs";
	public static final String PROCESS_STATS_NAME = "Process-Stats";
	@SuppressWarnings("unused")
	private boolean enableStats_elapsedTime;

	public Activity() {}

	public void init(ActivityContext context) throws ActivityException {}

	public abstract void destroy() throws Exception;

	public final void setURI(String uri) {
		this.uri = uri;
	}

	protected final String getURI() {
		return uri;
	}

	public XiNode eval(ProcessContext pc, XiNode inputData)
			throws ActivityException {
		return null;
	}

	public XiNode eval(ProcessContext pc, InputData inputData)
			throws ActivityException {
		try {
			XiNode input = null;
			if (inputData != null) {
				input = inputData.getXiNode();
				inputData.assertNoErrors();
			}
			return eval(pc, input);
		} catch (SAXException e) {
			return null;
		}
	}

	public XiNode postEval(ProcessContext pc, Object closure)
			throws ActivityException {
		return null;
	}

	/**
	 * @deprecated Method postEval is deprecated
	 */

	public XiNode postEval(ProcessContext pc, XiNode inputData, Object closure)
			throws ActivityException 
	{
		return null;
	}

	public boolean cancelled(ProcessContext pc) throws ActivityException {
		return true;
	}

	@SuppressWarnings("rawtypes")
	public List getReferencedVariables() {
		return null;
	}

	public String getInputVariableName() {
		return null;
	}

	public String getClassName() {
		return ((Object) this).getClass().getName();
	}

	public void setConfigParms(XiNode configParms, RepoAgent repoAgent)
			throws ActivityException {}

	public void setConfigParms(XiNode configParms, RepoAgent repoAgent,
			ProcessDefinitionContext procDef) throws ActivityException {
		
	}

	public ConfigError[] getConfigErrors() {
		return null;
	}

	public XiNode getConfigParms() {
		return configParms;
	}

	public SmElement getConfigClass() {
		return null;
	}

	public SmElement getReplyClass() {
		return null;
	}

	public SmElement[] getErrorClasses() {
		return null;
	}

	public SmElement getInputClass() {
		return null;
	}

	public SmElement getOutputClass() {
		return null;
	}

	public String getReplyGroup() {
		return null;
	}

	public String getConfirmGroup() {
		return null;
	}

	public Object getPartnerInfo() {
		return ((Object) (null));
	}

	public SmParticleTerm getOutputTerm() {
		return null;
	}

	public SmParticleTerm[] getErrorTerms() {
		return null;
	}

	public SmParticleTerm getInputTerm() {
		return null;
	}

	public boolean wantsAnnotatedInput() {
		return true;
	}

	public boolean wantsTypedInput() {
		return true;
	}

	public boolean wantsValidatedInput() {
		return true;
	}

	public boolean wantsOutputValidated() {
		return true;
	}

	public boolean elapsedTimeAlreadyPresent(SmElement outputType) {
		return false;
	}

	public boolean isElaspseTimeOn() {
		return false;
	}

	public void onRestart(ProcessContext processcontext,
			Serializable serializable) {
	}
	
}
