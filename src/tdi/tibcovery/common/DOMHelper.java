/*
 * Copyright (c) 2008-2011 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * By using this file and API you are obligated to use GPL licence
 * for your code.
 */

package tdi.tibcovery.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DOMHelper {
	
	public final static String XML_HEADER = "<?xml version=\"1.0\"?>";
	public static final String CDATA_START = "<![CDATA[";
	public static final String CDATA_END = "]]>";
	private static DocumentBuilder builder = null;
	private static DocumentBuilder builderNs = null;
		
	static {
		try {
	      builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	      DocumentBuilderFactory factoryNs = DocumentBuilderFactory.newInstance();
	      factoryNs.setNamespaceAware(true);
	      builderNs = factoryNs.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static class XMLString {
		private String s = null;
		public XMLString(String s) {
			this.s  = s;
		}
		public String toString() {
			return XML_HEADER+s;
		}
	}
	
	public static String getNameAttrib(Node nd) {
		if (nd==null)
			return null;
		NamedNodeMap nnm = nd.getAttributes();
		Node attr = nnm!=null ? nnm.getNamedItem("name") : null;
		return attr!=null ? attr.getNodeValue() : null;
	}
	
	public final static Document getEmptyDoc() {
		try {
			return DOMHelper.documentFromString(XML_HEADER+"<root/>");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String getValueAttrib(Node nd) {
		if (nd==null)
			return null;
		NamedNodeMap nnm = nd.getAttributes();
		Node attr = nnm!=null ? nnm.getNamedItem("value") : null;
		return attr!=null ? attr.getNodeValue() : null;
	}
	
	public static List<Node> getElementsByTag(Node parent, String tag) {
		LinkedList<Node> list = new LinkedList<Node>();
		if (parent!=null) {
			NodeList nodes = parent.getChildNodes();
			if (nodes!=null && nodes.getLength()>0) {
				for (int i=0; i < nodes.getLength(); i++) {
					Node nd = nodes.item(i);
					String ndName = nd.getNodeName();
					if (nd!=null && ndName!=null && tag.compareTo( ndName )==0 )
						list.add(nd);
				}
			}
		}
		return list;
	}
	
	public static List<Node> getElementsByTagNs(Node parent, String tag) {
		LinkedList<Node> list = new LinkedList<Node>();
		if (parent!=null) {
			NodeList nodes = parent.getChildNodes();
			if (nodes!=null && nodes.getLength()>0) {
				for (int i=0; i < nodes.getLength(); i++) {
					Node nd = nodes.item(i);
					String ndName = nd.getLocalName();
					if (nd!=null && ndName!=null && tag.compareTo( ndName )==0 )
						list.add(nd);
				}
			}
		}
		return list;
	}
	
	public static Node getElementByTagNs(Node parent, String tag, int idx) {
		LinkedList<Node> list = new LinkedList<Node>();
		if (parent!=null) {
			NodeList nodes = parent.getChildNodes();
			if (nodes!=null && nodes.getLength()>0) {
				for (int i=0; i < nodes.getLength(); i++) {
					Node nd = nodes.item(i);
					String ndName = nd.getLocalName();
					if (nd!=null && ndName!=null && tag.compareTo( ndName )==0 )
						list.add(nd);
				}
			}
		}
		if (idx >= 0 && idx < list.size())
			return list.get(idx);
		return null;
	}
	
	public static Node getElementByTag(Node parent, String tag, int idx) {
		LinkedList<Node> list = new LinkedList<Node>();
		if (parent!=null) {
			NodeList nodes = parent.getChildNodes();
			if (nodes!=null && nodes.getLength()>0) {
				for (int i=0; i < nodes.getLength(); i++) {
					Node nd = nodes.item(i);
					String ndName = nd.getNodeName();
					if (nd!=null && ndName!=null && tag.compareTo( ndName )==0 )
						list.add(nd);
				}
			}
		}
		if (idx >= 0 && idx < list.size())
			return list.get(idx);
		return null;
	}
	
	public static List<Node> getNestedElementsByTagNs(Node parent, String tag) {
		LinkedList<Node> list = new LinkedList<Node>();
		if (parent!=null) {
			NodeList nodes = parent.getChildNodes();
			if (nodes!=null && nodes.getLength()>0) {
				for (int i=0; i < nodes.getLength(); i++) {
					Node nd = nodes.item(i);
					String ndName = nd.getLocalName();
					if (nd!=null && ndName!=null && ndName.equals(tag) )
						list.add(nd);
					List<Node> ll = getNestedElementsByTagNs(nd, tag);
					list.addAll(ll);
				}
			}
		}
		return list;
	}
	
	public static List<Node> getElementsByTagAndNameNs(Node parent, String tag, String name) {
		LinkedList<Node> list = new LinkedList<Node>();
		if (parent!=null) {
			NodeList nodes = parent.getChildNodes();
			if (nodes!=null && nodes.getLength()>0) {
				for (int i=0; i < nodes.getLength(); i++) {
					Node nd = nodes.item(i);
					String ndName = nd.getLocalName();
					if (nd!=null && ndName!=null && tag.compareTo( ndName )==0 ) {
						String nm = getNameAttrib(nd);
						if (nm!=null && name!=null && name.compareTo( nm )==0) {
							list.add(nd);
						}
						else if (nm==null && name==null) {
							list.add(nd);
						}
					}
				}
			}
		}
		return list;
	}
	
	public static List<Node> getElementsByTagAndName(Node parent, String tag, String name) {
		LinkedList<Node> list = new LinkedList<Node>();
		if (parent!=null) {
			NodeList nodes = parent.getChildNodes();
			if (nodes!=null && nodes.getLength()>0) {
				for (int i=0; i < nodes.getLength(); i++) {
					Node nd = nodes.item(i);
					String ndName = nd.getNodeName();
					if (nd!=null && ndName!=null && tag.compareTo( ndName )==0 ) {
						String nm = getNameAttrib(nd);
						if (nm!=null && name!=null && name.compareTo( nm )==0) {
							list.add(nd);
						}
						else if (nm==null && name==null) {
							list.add(nd);
						}
					}
				}
			}
		}
		return list;
	}

	public static String getAttrib(Node nd, String name) {
		NamedNodeMap nnm = nd.getAttributes();
		Node attr = nnm!=null ? nnm.getNamedItem(name) : null;
		return attr!=null ? attr.getNodeValue() : null;	   
   }
	
	public static String getAttribNs(Node nd, String name, String ns) {
		NamedNodeMap nnm = nd.getAttributes();
		Node attr = nnm!=null ? nnm.getNamedItemNS(name, ns) : null;
		return attr!=null ? attr.getNodeValue() : null;
	}
	
	public static String nodeToString(Node node) throws Exception {
		Source source = new DOMSource(node);
        StringWriter stringWriter = new StringWriter();
        Result result = new StreamResult(stringWriter);
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(source, result);
        return stringWriter.getBuffer().toString();
	}
	
	public static void nodeToFile(Node node, File file) throws Exception {
		Source source = new DOMSource(node);
        FileWriter writer = new FileWriter(file);
        Result result = new StreamResult(writer);
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(source, result);
        writer.flush();       
	}
	
	public static String documentToString(Document doc) throws Exception {
		return nodeToString(doc);       
	}
	
	public static String nodeToXMLString(Node node) throws Exception {
		return new XMLString(nodeToString(node)).toString();
	}
	
	public static Document documentFromString(String xml) throws ParserConfigurationException, SAXException, IOException {
		return builder.parse(new InputSource(new StringReader(xml)));
	}
	
	public static Document documentNsFromString(String xml) throws ParserConfigurationException, SAXException, IOException {
		return builderNs.parse(new InputSource(new StringReader(xml)));
	}
	
	public static Document documentNsFromFile(File path) throws ParserConfigurationException, SAXException, IOException {
		return builderNs.parse(new InputSource(new FileInputStream(path)));
	}
	
	public static String escapeXmlOut(String s) {
		s = s.replaceAll("\\&", "&amp;");
		s = s.replaceAll("\\<", "&lt;");
		s = s.replaceAll("\\>", "&gt;");
		s = s.replaceAll("\\\"", "&quot;");
		return s;
	}
	
	public static String escapeXmlIn(String s) {
		s = s.replaceAll("&amp;", "&");
		s = s.replaceAll("&lt;", "<");
		s = s.replaceAll("&gt;", ">");
		s = s.replaceAll("&quot;", "\"");
		return s;
	}

	public static List<Node> getElementsByPathNs(Node nd, String path) {
		if (path==null)
			return null;
		List<Node> list = null;
		for (String s : path.split("\\/")) {
			list = getElementsByTagNs(nd, s);
			if (list.size()==0)
				break;
			else
				nd = list.get(0);
		}
		return list;
	}

	public static String getNodeValueNs(Node parent, String tag) {
		if (parent==null || tag==null)
			return null;
		List<Node> list = getElementsByTagNs(parent, tag);
		if (list.size()==0)
			return null;
		return list.get(0).getTextContent();
	}	
}
