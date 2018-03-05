package tdi.core;

public class NodeTextExtractor {
	
	public final static String getText(Object nd) {
		if (nd instanceof com.tibco.xml.datamodel.nodes.Text)
			return ((com.tibco.xml.datamodel.nodes.Text)nd).getStringValue();
		if (nd instanceof com.tibco.xml.datamodel.nodes.XiDefaultNode)
			return ((com.tibco.xml.datamodel.nodes.XiDefaultNode)nd).getStringValue();
		if (nd instanceof org.w3c.dom.Node)
			return ((org.w3c.dom.Node)nd).getNodeValue();
		String s = nd+"";
		if (!s.startsWith(nd.getClass().getName()))
			return s;
		return null;
	}
}
