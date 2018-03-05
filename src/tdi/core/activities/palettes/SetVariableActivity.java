package tdi.core.activities.palettes;

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

public class SetVariableActivity extends Activity {
	
	private static final long serialVersionUID = 1L;
	static {
		tdi.stub.init();
	}

	public SetVariableActivity() {}

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
				com.tibco.xml.datamodel.nodes.Text name = (Text) root.getFirstChild();
				com.tibco.xml.datamodel.nodes.Text xml = (Text) (name.hasNextSibling() ? name.getNextSibling() : null);
				if (xml!=null && xml.getStringValue()!=null)
					tdi.apiHome.INSTANCE().SetJobVariableFromXml(pc.getId(), name.getStringValue(), xml.getStringValue());				
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
			schema.setNamespace("http://www.tibco.com/pe/TDI/SetVariableActivitySchema");
			MutableType iType = MutableSupport.createType(schema, "VariableXmlType");
			MutableSupport.addRequiredLocalElement(iType, "name", XSDL.STRING);
			MutableSupport.addRequiredLocalElement(iType, "xml", XSDL.STRING);			
			
			INPUT_TYPE = MutableSupport.createElement(schema, "ActivityInput", iType);
			
			schema.lock(new DefaultSchemaErrorHandler());
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}		
	}
}
