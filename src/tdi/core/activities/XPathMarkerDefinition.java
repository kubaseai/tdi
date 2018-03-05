package tdi.core.activities;

import java.util.LinkedList;
import java.util.List;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NodeList;

import com.tibco.xml.datamodel.XiNode;

import tdi.core.Logger;
import tdi.core.XiNodeWrapper;

public class XPathMarkerDefinition extends MarkerDefinition {
	
	private transient volatile NodeList result = null;
	private XPathExpression expression = null;
	private boolean simpleXpath = true;
	private final static javax.xml.xpath.XPathFactory factory = prepareFactory();

	public XPathMarkerDefinition(String name, String s) {
		if (s!=null && s.startsWith("xpath:")) {
			s = s.substring(6);
			simpleXpath = false;
		}
		this.path = s!=null ? s : "";
		this.markerName = name;
		try {			
			this.expression = factory.newXPath().compile(s);
		}
		catch (Exception e) {
			Logger.getInstance().debug("XPathFactory error", e);
		}
		
	}

	private static XPathFactory prepareFactory() {
		return new com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl(true);	
	}

	public boolean matches(XiNode nd) {
		if (expression==null || nd==null)
			return false;
		try {
			result = (NodeList) expression.evaluate(new XiNodeWrapper(nd, simpleXpath), XPathConstants.NODESET);
			return result!=null && result.getLength() > 0;
		} 
		catch (XPathExpressionException e) {
			return false;
		}
		catch (Throwable t) {
			Logger.getInstance().debug("XPath libraries error", t);
			return false;
		}
	}

	public List<String> getMatches() {
		LinkedList<String> list = new LinkedList<String>();
		if (result!=null) {
			for (int i=0; i < result.getLength(); i++) {
				String s = result.item(i).getNodeValue();
				if (s!=null)
					list.add(s);
			}
		}
		return list;
	}
}
