package tdi.core.activities.palettes;

import com.tibco.bw.store.RepoAgent;
import com.tibco.pe.plugin.Activity;
import com.tibco.pe.plugin.ActivityContext;
import com.tibco.pe.plugin.ActivityException;
import com.tibco.pe.plugin.ProcessContext;
import com.tibco.xml.datamodel.XiNode;
import com.tibco.xml.schema.SmElement;
import com.tibco.xml.schema.SmFactory;
import com.tibco.xml.schema.build.MutableSchema;
import com.tibco.xml.schema.build.MutableSupport;
import com.tibco.xml.schema.build.MutableType;
import com.tibco.xml.schema.flavor.XSDL;
import com.tibco.xml.xdata.DefaultSchemaErrorHandler;

public class InjectGlobalVariablesActivity extends Activity {
	
	private static final long serialVersionUID = 1L;
	static {
		tdi.stub.init();
	}

	public InjectGlobalVariablesActivity() {}

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
		if (inputData!=null && inputData.hasChildNodes()) {
			XiNode root = inputData.getFirstChild();
			if (root!=null && root.hasChildNodes()) {
				XiNode xml = root.getFirstChild();
				String gvXml = ((com.tibco.xml.datamodel.nodes.Text)xml).getStringValue();
				if (gvXml!=null) {
					tdi.apiHome.INSTANCE().ReplaceGlobalVariables(gvXml);
				}
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
			schema.setNamespace("http://www.tibco.com/pe/TDI/InjectGlobalVariablesActivitySchema");
			MutableType iType = MutableSupport.createType(schema, "WrappedXmlType");	
			MutableSupport.addOptionalLocalElement(iType, "xml", XSDL.STRING);			
			
			MutableType inputType = MutableSupport.createType(schema, "ActivityInputSchema");
			MutableSupport.addRequiredLocalElement(inputType, "Vars", iType);
			
			INPUT_TYPE = MutableSupport.createElement(schema, "ActivityInput", inputType);
			
			schema.lock(new DefaultSchemaErrorHandler());
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}		
	}
}
