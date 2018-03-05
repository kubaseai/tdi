package tdi.core.activities.palettes;

import java.io.StringReader;
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

public class GetProcessesFromDirActivity extends Activity {
	
	private static final long serialVersionUID = 1L;
	static {
		tdi.stub.init();
	}

	public GetProcessesFromDirActivity() {}

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
		return OUTPUT_TYPE;
	}

	public XiNode eval(ProcessContext pc, XiNode inputData)
			throws ActivityException {
		StringBuilder xml = new StringBuilder("<?xml version=\"1.0\"?><ns0:ActivityOutput xmlns:ns0=\"http://www.tibco.com/pe/TDI/GetProcessesFromDirActivitySchema\">");
		if (inputData!=null && inputData.hasChildNodes()) {
			XiNode root = inputData.getFirstChild();
			if (root!=null && root.hasChildNodes()) {
				XiNode name = root.getFirstChild();
				for (String file : tdi.apiHome.INSTANCE().GetProcessesFromDir(((com.tibco.xml.datamodel.nodes.Text)name).getStringValue())) {
					xml.append("<process>").append(file).append("</process>");
				}
			}			
		}
		xml.append("</ns0:ActivityOutput>");
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
	private static final SmElement OUTPUT_TYPE;
		
	static {
		MutableSchema schema = null;
		try {
			schema = SmFactory.newInstance().createMutableSchema();
			schema.setNamespace("http://www.tibco.com/pe/TDI/GetProcessesFromDirActivitySchema");
			MutableType iType = MutableSupport.createType(schema, "GetProcessesFromDirInputType");
			MutableSupport.addRequiredLocalElement(iType, "dir", XSDL.STRING);
			INPUT_TYPE = MutableSupport.createElement(schema, "ActivityInput", iType);
			
			MutableType oType = MutableSupport.createType(schema, "GetProcessesFromDirOutputType");
			MutableSupport.addRepeatingLocalElement(oType, "process", XSDL.STRING);
			OUTPUT_TYPE = MutableSupport.createElement(schema, "ActivityOutput", oType);
			
			schema.lock(new DefaultSchemaErrorHandler());
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}		
	}
}
