package tdi.core.activities.palettes;

import tdi.apiHome;

import com.tibco.bw.store.RepoAgent;
import com.tibco.pe.plugin.Activity;
import com.tibco.pe.plugin.ActivityContext;
import com.tibco.pe.plugin.ActivityException;
import com.tibco.pe.plugin.ProcessContext;
import com.tibco.xml.datamodel.XiNode;
import com.tibco.xml.datamodel.nodes.Text;
import com.tibco.xml.schema.SmElement;
import com.tibco.xml.schema.SmFactory;
import com.tibco.xml.schema.build.MutableSchema;
import com.tibco.xml.schema.build.MutableSupport;
import com.tibco.xml.schema.build.MutableType;
import com.tibco.xml.schema.flavor.XSDL;
import com.tibco.xml.xdata.DefaultSchemaErrorHandler;

public class SetupMocksActivity extends Activity {

	private static final long serialVersionUID = 1L;
	static {
		tdi.stub.init();
	}

	public SetupMocksActivity() {}

	public void setConfigParms(XiNode configParms, RepoAgent repoAgent)
			throws ActivityException {
		super.setConfigParms(configParms, repoAgent);		
	}

	public void init(ActivityContext ctx) throws ActivityException {
		super.init(ctx);		
	}

	public SmElement getInputClass() {
		return INPUT_TYPE;
	}

	public SmElement getOutputClass() {
		return null;
	}

	public XiNode eval(ProcessContext pc, XiNode inputData)
			throws ActivityException {
		apiHome.INSTANCE().instrument(null);
		if (inputData!=null && inputData.hasChildNodes()) {
			XiNode root = inputData.getFirstChild();
			if (root!=null && root.hasChildNodes()) {
				XiNode rule = root.getFirstChild();
				do {
					String process = null;
					String activity = null;
					String xmlReplacement = null;
					String processReplacement = null;
					if (rule.hasChildNodes()) {
						com.tibco.xml.datamodel.nodes.Text child = (Text) rule.getFirstChild();
						do {
							String name = child.getName()!=null ? child.getName().localName : null;
							if ("Process".equals(name)){
								process = child.getStringValue();
							}
							else if ("Activity".equals(name)) {
								activity = child.getStringValue();
							}
							else if ("XmlReplacement".equals(name)) {
								xmlReplacement = child.getStringValue();
							}
							else if ("ProcessReplacement".equals(name)) {
								processReplacement = child.getStringValue();
							}
							child = (Text) (child.hasNextSibling() ? child.getNextSibling() : null);
						}
						while (child!=null);
						
					}
					apiHome.INSTANCE().AddMockingRule(process, activity, xmlReplacement!=null ? xmlReplacement : processReplacement);
					rule = rule.hasNextSibling() ? rule.getNextSibling() : null;
				}
				while (rule != null);
			}			
		}
		return null;
	}

	public void destroy() throws Exception {
		;
	}

	private static final SmElement INPUT_TYPE;
		
	static {
		MutableSchema schema = null;
		try {
			schema = SmFactory.newInstance().createMutableSchema();
			schema.setNamespace("http://www.tibco.com/pe/TDI/SetupMocksActivitySchema");
			MutableType ruleType = MutableSupport.createType(schema, "ReplacementRuleType");
			MutableSupport.addRequiredLocalElement(ruleType, "Process", XSDL.STRING);
			MutableSupport.addRequiredLocalElement(ruleType, "Activity", XSDL.STRING);
			MutableSupport.addOptionalLocalElement(ruleType, "XmlReplacement", XSDL.STRING);
			MutableSupport.addOptionalLocalElement(ruleType, "ProcessReplacement", XSDL.STRING);
			
			MutableType inputType = MutableSupport.createType(schema, "ActivityInputSchema");
			MutableSupport.addRepeatingLocalElement(inputType, "rule", ruleType);
			
			INPUT_TYPE = MutableSupport.createElement(schema, "ActivityInput", inputType);
			
			schema.lock(new DefaultSchemaErrorHandler());
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}		
	}
}
