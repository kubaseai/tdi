package tdi.core.activities.palettes;

import java.io.StringReader;
import java.util.List;

import org.xml.sax.InputSource;

import com.tibco.bw.store.RepoAgent;
import com.tibco.pe.plugin.Activity;
import com.tibco.pe.plugin.ActivityContext;
import com.tibco.pe.plugin.ActivityException;
import com.tibco.pe.plugin.ProcessContext;
import com.tibco.xml.datamodel.XiNode;
import com.tibco.xml.datamodel.XiParserFactory;
import com.tibco.xml.schema.SmElement;
import com.tibco.xml.schema.SmFactory;
import com.tibco.xml.schema.build.MutableSchema;
import com.tibco.xml.schema.build.MutableSupport;
import com.tibco.xml.schema.build.MutableType;
import com.tibco.xml.schema.flavor.XSDL;
import com.tibco.xml.xdata.DefaultSchemaErrorHandler;

public class InitActivity extends Activity {
	
	private static final long serialVersionUID = 1L;

	public InitActivity() {}

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
		return INPUT_TYPE;
	}

	public XiNode eval(ProcessContext pc, XiNode inputData)
			throws ActivityException {
		List<String> params = tdi.apiHome.INSTANCE().GetConfigParameters();
		tdi.apiHome.INSTANCE().instrument(null);
		StringBuilder xml = new StringBuilder("<?xml version=\"1.0\"?><i:ActivityInput xmlns:i=\"http://www.tibco.com/pe/TDI/InitActivitySchema\">");
		for (String p : params) {
			int pos = p.indexOf(",");
			String key = p.substring(6, pos);
			String value = p.substring(pos+2, p.lastIndexOf(')'));
			xml.append("<Config><key>").append(key).append("</key><value>").append(value).append("</value></Config>");
		}
		xml.append("</i:ActivityInput>");
		XiNode activityInput = inputData.hasChildNodes() ? inputData.getFirstChild() : null;
		if (activityInput!=null) {
			XiNode config = activityInput.hasChildNodes() ? activityInput.getFirstChild() : null;
			while (config!=null) {
				XiNode key = config.hasChildNodes() ? config.getFirstChild() : null;
				XiNode val = key!=null && key.hasNextSibling() ? key.getNextSibling() : null;
				if (key!=null && val!=null) {
					tdi.apiHome.INSTANCE().setRuntimeProperty(((com.tibco.xml.datamodel.nodes.Text)key).getStringValue(), ((com.tibco.xml.datamodel.nodes.Text)val).getStringValue());
				}					
				config = config.hasNextSibling() ? config.getNextSibling() : null;
			}
		}
		
		try {
			return XiParserFactory.newInstance().parse(new InputSource(new StringReader(xml.toString())));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void destroy() throws Exception {
		;
	}

	private static final SmElement INPUT_TYPE;
		
	static {
		MutableSchema schema = null;
		try {
			schema = SmFactory.newInstance().createMutableSchema();
			schema.setNamespace("http://www.tibco.com/pe/TDI/InitActivitySchema");
			MutableType iType = MutableSupport.createType(schema, "PropertyType");	
			MutableSupport.addOptionalLocalElement(iType, "key", XSDL.STRING);	
			MutableSupport.addOptionalLocalElement(iType, "value", XSDL.STRING);
			
			MutableType inputType = MutableSupport.createType(schema, "ActivityInputSchema");
			MutableSupport.addRepeatingLocalElement(inputType, "Config", iType);
			
			INPUT_TYPE = MutableSupport.createElement(schema, "ActivityInput", inputType);
			
			schema.lock(new DefaultSchemaErrorHandler());
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}		
	}
}
