package tdi.core.activities.palettes;

import java.util.HashMap;

import com.tibco.ae.designerapi.DesignerDocument;
import com.tibco.ae.designerapi.forms.ConfigForm;
import com.tibco.ae.processapi.BWActivityResource;

public class GenericActivityResource extends BWActivityResource {
	
	protected final static HashMap<String,String> TYPE_MAP = new HashMap<String,String>();
	
	public GenericActivityResource() {
		super(true);		
	}

	public GenericActivityResource(boolean flag) {
		super(flag);
	}

	public String getResourceType() {
		return TYPE_MAP.get(this.getClass().getName());
	}

	public boolean wantsInputView() {
		return true;
	}

	public boolean wantsOutputView() {
		return true;
	}

	public boolean wantsInputBindingsInitialized() {
		return true;
	}
	
	public void initModel() throws Exception {
		super.initModel();		
	}

	public void buildConfigurationForm(ConfigForm configform,
			DesignerDocument designerdocument) {
		super.buildConfigurationForm(configform, designerdocument);		
	}
}
