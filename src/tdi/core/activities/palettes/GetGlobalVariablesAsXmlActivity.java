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

public class GetGlobalVariablesAsXmlActivity extends Activity {
	
	static {
		tdi.stub.init();
	}
	
	private static final long serialVersionUID = 1L;

	public GetGlobalVariablesAsXmlActivity() {}

	public void setConfigParms(XiNode configParms, RepoAgent repoAgent)
			throws ActivityException {
		super.setConfigParms(configParms, repoAgent);		
	}

	public void init(ActivityContext ctx) throws ActivityException {
		super.init(ctx);		
	}

	public SmElement getInputClass() {
		return null;
	}

	public SmElement getOutputClass() {
		return OUTPUT_TYPE;
	}

	public XiNode eval(ProcessContext pc, XiNode inputData)
			throws ActivityException {
		String xml = tdi.apiHome.INSTANCE().GetJobVariableAsXml(0, "_globalVariables");
		String res = "<ns0:ActivityOutput xmlns:ns0=\"http://www.tibco.com/pe/TDI/GetGlobalVariablesAsXmlActivitySchema\">"+
		"<Vars><xml><![CDATA["+xml+"]]></xml></Vars></ns0:ActivityOutput>";
		try {
			return XiParserFactory.newInstance().parse(new InputSource(new StringReader("<?xml version=\"1.0\"?>"+res)));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void destroy() throws Exception {
		;
	}

	private static final SmElement OUTPUT_TYPE;
		
	static {
		MutableSchema schema = null;
		try {
			schema = SmFactory.newInstance().createMutableSchema();
			schema.setNamespace("http://www.tibco.com/pe/TDI/GetGlobalVariablesAsXmlActivitySchema");
			MutableType oType = MutableSupport.createType(schema, "WrapperXmlType");	
			MutableSupport.addOptionalLocalElement(oType, "xml", XSDL.STRING);			
			
			MutableType outputType = MutableSupport.createType(schema, "ActivityOutputSchema");
			MutableSupport.addRequiredLocalElement(outputType, "Vars", oType);
			
			OUTPUT_TYPE = MutableSupport.createElement(schema, "ActivityOutput", outputType);
			
			schema.lock(new DefaultSchemaErrorHandler());
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}		
	}
}

