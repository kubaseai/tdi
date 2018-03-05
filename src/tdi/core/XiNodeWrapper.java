package tdi.core;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.InputSource;

import com.tibco.xml.data.primitive.ExpandedName;
import com.tibco.xml.data.primitive.NamespaceToPrefixResolver.NamespaceNotFoundException;
import com.tibco.xml.data.primitive.PrefixToNamespaceResolver.PrefixNotFoundException;
import com.tibco.xml.data.primitive.XmlNodeKind;
import com.tibco.xml.datamodel.XiNode;
import com.tibco.xml.datamodel.XiParserFactory;

import tdi.core.activities.XPathMarkerDefinition;


public class XiNodeWrapper extends DOMSource implements org.w3c.dom.Node, org.w3c.dom.Element,
	org.w3c.dom.Attr, org.w3c.dom.CharacterData, org.w3c.dom.Comment, org.w3c.dom.CDATASection,
	org.w3c.dom.ProcessingInstruction, org.w3c.dom.Document, org.w3c.dom.DocumentType {
	
	@Override
	public Node getNode() {
		return this;
	}

	@Override
	public void setNode(Node node) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	@Override
	public void setSystemId(String systemID) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	private XiNode xi = null;
	private boolean ignoreNamespaces = true;
	private final static Node referenceNode = makeReferenceNode();
	private HashMap<String,Object> userData = new HashMap<String,Object>();
	private final static NamedNodeMap EMPTY_NAMED_NODE_MAP = new NamedNodeMap() {
		
		public Node setNamedItemNS(Node node) throws DOMException {
			throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
		}
		
		public Node setNamedItem(Node node) throws DOMException {
			throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
		}
		
		
		public Node removeNamedItemNS(String s, String s1) throws DOMException {
			return null;
		}
		
		
		public Node removeNamedItem(String s) throws DOMException {
			return null;
		}
		
		
		public Node item(int i) {				
			return null;
		}
		
		
		public Node getNamedItemNS(String s, String s1) throws DOMException {
			return null;
		}
		
		
		public Node getNamedItem(String s) {
			return null;
		}
		
		
		public int getLength() {				
			return 0;
		}
	};
	
	public XiNodeWrapper(XiNode xi) {
		if (xi==null) {
			try {
				xi = XiParserFactory.newInstance().parse(new InputSource(new StringReader("<?xml version=\"1.0\"?><root/>")));
			}
			catch (Exception e) {}
		}
		this.xi = xi;		
	}

	public XiNodeWrapper(XiNode nd, boolean ignoreNamespaces) {
		this(nd);
		this.ignoreNamespaces = ignoreNamespaces;
	}		

	private static Node makeReferenceNode() {
		try {
			DocumentBuilderFactory factoryNs = DocumentBuilderFactory.newInstance();
		    factoryNs.setNamespaceAware(true);
		    DocumentBuilder builderNs = factoryNs.newDocumentBuilder();
			Document doc = builderNs.parse(new InputSource(new StringReader("<?xml version=\"1.0\"?><root/>")));
			return doc.getFirstChild();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
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

	
	public String getAttribute(String name) {
		return xi.getAttributeStringValue(ExpandedName.makeName(name));		
	}

	
	public String getAttributeNS(String namespaceURI, String localName)
			throws DOMException {
		if (ignoreNamespaces)
			namespaceURI = null;
		return xi.getAttributeStringValue(ExpandedName.makeName(namespaceURI, localName));		
	}

	
	public Attr getAttributeNode(String name) {
		return new XiNodeWrapper(xi.getAttribute(ExpandedName.makeName(name)), ignoreNamespaces);
	}

	
	public Attr getAttributeNodeNS(String namespaceURI, String localName)
			throws DOMException {
		if (ignoreNamespaces)
			namespaceURI = null;
		return new XiNodeWrapper(xi.getAttribute(ExpandedName.makeName(namespaceURI, localName)), ignoreNamespaces);
	}

	
	public NodeList getElementsByTagName(String name) {
		final LinkedList<XiNodeWrapper> list = new LinkedList<XiNodeWrapper>();
		NodeList ndList = new NodeList() {			
			
			public Node item(int index) {
				return list.get(index);
			}			
			
			public int getLength() {
				return list.size();
			}
		};
		
		if (xi.hasChildNodes()) {
			XiNode node = xi.getFirstChild();
			do {
				if ((node.getName().localName+"").equals(name+"")) {
					list.add(new XiNodeWrapper(node, ignoreNamespaces));
				}
				node = node.hasNextSibling() ? node.getNextSibling() : null;
			}
			while (node!=null);
		}
		return ndList;
	}

	
	public NodeList getElementsByTagNameNS(String namespaceURI, String localName)
			throws DOMException {
		final LinkedList<XiNodeWrapper> list = new LinkedList<XiNodeWrapper>();
		NodeList ndList = new NodeList() {			
			
			public Node item(int index) {
				return list.get(index);
			}			
			
			public int getLength() {
				return list.size();
			}
		};
		
		if (xi.hasChildNodes()) {
			XiNode node = xi.getFirstChild();
			do {
				if (node.getName()==null)
					;
				else if ((node.getName().localName+"").equals(localName+"") &&
					((node.getName().namespaceURI+"").equals(namespaceURI+"") || ignoreNamespaces))
				{
					list.add(new XiNodeWrapper(node, ignoreNamespaces));
				}
				node = node.hasNextSibling() ? node.getNextSibling() : null;
			}
			while (node!=null);
		}
		return ndList;
	}

	
	public TypeInfo getSchemaTypeInfo() {
		return new TypeInfo() {
			
			
			public boolean isDerivedFrom(String typeNamespaceArg, String typeNameArg,
					int derivationMethod) {
				return ((xi.getType().getBaseType().getNamespace()+"").equals(typeNamespaceArg) || ignoreNamespaces) &&
					(xi.getType().getBaseType().getName()+"").equals(typeNameArg);
			}
			
			
			public String getTypeNamespace() {
				return ignoreNamespaces ? "" : xi.getType().getNamespace();
			}
			
			
			public String getTypeName() {
				return xi.getType().getName();
			}
		};
	}

	
	public String getTagName() {
		return xi.getName()!=null ? xi.getName().localName : null;
	}

	
	public boolean hasAttribute(String name) {
		return getAttribute(name)!=null;
	}

	
	public boolean hasAttributeNS(String namespaceURI, String localName)
			throws DOMException {
		return getAttributeNS(namespaceURI,localName)!=null;
	}

	
	public void removeAttribute(String name) throws DOMException {
		xi.removeAttribute(ExpandedName.makeName(name));		
	}

	
	public void removeAttributeNS(String namespaceURI, String localName)
			throws DOMException {
		if (ignoreNamespaces)
			namespaceURI = null;
		xi.removeAttribute(ExpandedName.makeName(namespaceURI, localName));		
	}

	
	public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
		String namespaceURI = ignoreNamespaces ? null : oldAttr.getNamespaceURI();
		XiNode removed = xi.removeAttribute(ExpandedName.makeName(namespaceURI, oldAttr.getLocalName()));
		return removed != null ? oldAttr : null;
	}

	
	public void setAttribute(String name, String value) throws DOMException {
		xi.setAttributeStringValue(ExpandedName.makeName(name), value);		
	}

	
	public void setAttributeNS(String namespaceURI, String qualifiedName,
			String value) throws DOMException {
		if (ignoreNamespaces)
			namespaceURI = null;
		xi.setAttributeStringValue(ExpandedName.makeName(namespaceURI, qualifiedName), value);			
	}

	
	public Attr setAttributeNode(Attr newAttr) throws DOMException {
		setAttribute(newAttr.getName(), newAttr.getValue());
		return newAttr;
	}

	
	public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
		String namespaceURI = ignoreNamespaces ? null : newAttr.getNamespaceURI();
		setAttributeNS(namespaceURI , newAttr.getName(), newAttr.getValue());
		return newAttr;
	}

	
	public void setIdAttribute(String name, boolean isId) throws DOMException {		
	}
	
	public void setIdAttributeNS(String namespaceURI, String localName,
			boolean isId) throws DOMException {			
	}
	
	public void setIdAttributeNode(Attr idAttr, boolean isId)
			throws DOMException {
	}

	
	public Node appendChild(Node newChild) throws DOMException {
		if (!(newChild instanceof XiNodeWrapper))
			throw new DOMException(DOMException.TYPE_MISMATCH_ERR, "Trying to add incompatible child node");
		xi.appendChild(((XiNodeWrapper)newChild).xi);
		return newChild;
	}

	
	public Node cloneNode(boolean deep) {
		return new XiNodeWrapper(deep ? xi.copy() : xi, ignoreNamespaces);
	}

	
	public short compareDocumentPosition(Node other) throws DOMException {
		throw new DOMException(DOMException.TYPE_MISMATCH_ERR, "Comparison of node position is not supported");
	}

	
	public NamedNodeMap getAttributes() {
		return new NamedNodeMap() {
			
			
			public Node setNamedItemNS(Node arg) throws DOMException {
				String ns = ignoreNamespaces ? null : arg.getNamespaceURI();
				XiNode result = xi.setAttributeStringValue(ExpandedName.makeName(
					ns, arg.getLocalName()), arg.getTextContent());
				return result!=null ? new XiNodeWrapper(result, ignoreNamespaces) : null;
			}
			
			
			public Node setNamedItem(Node arg) throws DOMException {
				setAttribute(arg.getLocalName(), arg.getTextContent());
				return arg;
			}
			
			
			@SuppressWarnings("unchecked")
			public Node removeNamedItemNS(String namespaceURI, String localName)
					throws DOMException {
				Iterator<XiNode> it = xi.getAttributes();
				XiNode toRemove = null;
				while (it.hasNext()) {
					XiNode current = it.next();
					if (current.getName() == null)
						;
					else if (((current.getName().getNamespaceURI()+"").equals(namespaceURI+"") || ignoreNamespaces) &&
						(current.getName().getLocalName()+"").equals(localName+"")) {
						toRemove = current;
						break;
					}
				}
				removeAttributeNS(namespaceURI, localName);
				return toRemove!=null ? new XiNodeWrapper(toRemove, ignoreNamespaces) : null;
			}
			
			
			@SuppressWarnings("unchecked")
			public Node removeNamedItem(String name) throws DOMException {
				Iterator<XiNode> it = xi.getAttributes();
				XiNode toRemove = null;
				while (it.hasNext()) {
					XiNode current = it.next();
					if (current.getName() == null)
						;
					else if ((current.getName().getLocalName()+"").equals(name+"")) {
						toRemove = current;
						break;
					}
				}
				removeAttribute(name);
				return toRemove!=null ? new XiNodeWrapper(toRemove, ignoreNamespaces) : null;
			}
			
			
			@SuppressWarnings("unchecked")
			public Node item(int index) {
				Iterator<XiNode> it = xi.getAttributes();
				int i = 0;
				while (it.hasNext()) {
					XiNode toReturn = it.next();
					if (i++==index)
						return new XiNodeWrapper(toReturn, ignoreNamespaces);					
				}
				return null;
			}
			
			
			public Node getNamedItemNS(String namespaceURI, String localName)
					throws DOMException {
				return getAttributeNodeNS(namespaceURI, localName);
			}
			
			
			public Node getNamedItem(String name) {
				return getAttributeNode(name);
			}
			
			
			@SuppressWarnings("unchecked")
			public int getLength() {
				int i = 0;
				Iterator<XiNode> it = xi.getAttributes();
				for (; it.hasNext(); i++) it.next();
				return i;
			}
		};		
	}

	
	public String getBaseURI() {
		return xi.getBaseURI();
	}

	
	public NodeList getChildNodes() {
		final LinkedList<XiNodeWrapper> list = new LinkedList<XiNodeWrapper>();
		NodeList ndList = new NodeList() {			
			
			public Node item(int index) {
				return list.get(index);
			}			
			
			public int getLength() {
				return list.size();
			}
		};
		
		if (xi.hasChildNodes()) {
			XiNode node = xi.getFirstChild();
			do {
				list.add(new XiNodeWrapper(node, ignoreNamespaces));
				node = node.hasNextSibling() ? node.getNextSibling() : null;				
			}
			while (node!=null);
		}
		return ndList;
	}

	
	public Object getFeature(String feature, String version) {
		return referenceNode.getFeature(feature, version);		
	}

	
	public Node getFirstChild() {
		return xi.hasChildNodes() ? new XiNodeWrapper(xi.getFirstChild(), ignoreNamespaces) : null;
	}

	
	public Node getLastChild() {
		return xi.hasChildNodes() ? new XiNodeWrapper(xi.getLastChild(), ignoreNamespaces) : null;
	}

	
	public String getLocalName() {
		return xi.getName()==null ? "" : xi.getName().localName;
	}

	
	public String getNamespaceURI() {
		if (ignoreNamespaces)
			return null;
		return xi.getName()==null ? null : xi.getName().namespaceURI;
	}

	
	public Node getNextSibling() {
		return xi.hasNextSibling() ? new XiNodeWrapper(xi.getNextSibling(), ignoreNamespaces) : null;
	}

	
	public String getNodeName() {
		return xi.getName()==null ? null : xi.getName().toString();
	}

	
	public short getNodeType() {
		if (xi.getNodeKind().equals(XmlNodeKind.ELEMENT))
			return Node.ELEMENT_NODE;
		if (xi.getNodeKind().equals(XmlNodeKind.ATTRIBUTE))
			return Node.ATTRIBUTE_NODE;
		if (xi.getNodeKind().equals(XmlNodeKind.COMMENT))
			return Node.COMMENT_NODE;		
		if (xi.getNodeKind().equals(XmlNodeKind.TEXT))
			return Node.TEXT_NODE;
		if (xi.getNodeKind().equals(XmlNodeKind.DOCUMENT))
			return Node.DOCUMENT_NODE;
		if (xi.getNodeKind().equals(XmlNodeKind.PROCESSING_INSTRUCTION))
			return Node.PROCESSING_INSTRUCTION_NODE;		
		if (xi.getNodeKind().equals(XmlNodeKind.FRAGMENT))
			return Node.DOCUMENT_FRAGMENT_NODE;
		if (xi.getNodeKind().equals(XmlNodeKind.NAMESPACE))
			return Node.ATTRIBUTE_NODE;
		return Node.PROCESSING_INSTRUCTION_NODE;
	}

	
	public String getNodeValue() throws DOMException {
		return xi.getStringValue();
	}
	
	public Document getOwnerDocument() {
		XiNode node = xi;
		while (node.getParentNode()!=null)
			node = node.getParentNode();
		return node == xi ? this : new XiNodeWrapper(node, ignoreNamespaces);			
	}
		
	public Node getParentNode() {
		XiNode p = xi.getParentNode();
		return p!=null ? new XiNodeWrapper(p, ignoreNamespaces) : null;
	}

	
	public String getPrefix() {
		return xi.getPrefix();
	}

	
	public Node getPreviousSibling() {
		XiNode p = xi.getPreviousSibling();
		return p!=null ? new XiNodeWrapper(p, ignoreNamespaces) : null;
	}

	
	public String getTextContent() throws DOMException {
		return xi.getStringValue();
	}

	
	public Object getUserData(String key) {
		return userData.get(key);
	}

	
	public boolean hasAttributes() {
		return xi.hasAttributes();
	}

	
	public boolean hasChildNodes() {
		return xi.hasChildNodes();
	}

	
	public Node insertBefore(Node newChild, Node refChild) throws DOMException {
		if (!(newChild instanceof XiNodeWrapper) && !(refChild instanceof XiNodeWrapper))
			throw new DOMException(DOMException.TYPE_MISMATCH_ERR, "Incompatible node types");
		XiNode result = xi.insertBefore(((XiNodeWrapper)newChild).xi, ((XiNodeWrapper)refChild).xi);
		return result!=null ? new XiNodeWrapper(result, ignoreNamespaces) : null;
	}

	
	public boolean isDefaultNamespace(String namespaceURI) {
		if (ignoreNamespaces)
			return namespaceURI==null || namespaceURI.length()==0;					
		return (xi.getDefaultNamespaceForElementAndTypeNames()+"").equals(namespaceURI+"");
	}

	
	public boolean isEqualNode(Node arg) {
		return isSameNode(arg);
	}

	
	public boolean isSameNode(Node other) {
		return (other instanceof XiNodeWrapper) && ((XiNodeWrapper)other).xi == xi;
	}

	
	public boolean isSupported(String feature, String version) {
		return referenceNode.isSupported(feature, version);
	}

	
	public String lookupNamespaceURI(String prefix) {
		try {
			return xi.getNamespaceURIForPrefix(prefix);
		}
		catch (PrefixNotFoundException e) {
			return null;
		}
	}

	
	public String lookupPrefix(String namespaceURI) {
		try {
			return xi.getPrefixForNamespaceURI(namespaceURI);
		}
		catch (NamespaceNotFoundException e) {
			return null;
		}
	}

	
	public void normalize() {		
	}

	
	public Node removeChild(Node oldChild) throws DOMException {
		if (!(oldChild instanceof XiNodeWrapper))
			throw new DOMException(DOMException.TYPE_MISMATCH_ERR, "Incompatible node types");
		XiNode result = xi.removeChild(((XiNodeWrapper)oldChild).xi, true);
		return result!=null ? new XiNodeWrapper(result, ignoreNamespaces) : null;
	}

	
	public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
		XiNodeWrapper result = (XiNodeWrapper) insertBefore(newChild, oldChild);
		removeChild(oldChild);
		return result;
	}

	
	public void setNodeValue(String nodeValue) throws DOMException {
		xi.setStringValue(nodeValue);		
	}

	
	public void setPrefix(String prefix) throws DOMException {
		if (xi.getName()!=null)
			xi.setNamespace(prefix, xi.getName().getNamespaceURI());
		
	}

	
	public void setTextContent(String textContent) throws DOMException {
		xi.setStringValue(textContent);		
	}

	
	public Object setUserData(String key, Object data, UserDataHandler handler) {
		return userData.put(key, data);
	}

	
	public String getName() {
		return xi.getName()==null ? "" : xi.getName().toString();
	}

	
	public Element getOwnerElement() {
		if (xi.getParentNode()!=null && xi.getNodeKind().equals(XmlNodeKind.ATTRIBUTE))
			return (Element) getParentNode();
		return null;
	}

	
	public boolean getSpecified() {
		return xi.getNodeKind().equals(XmlNodeKind.ATTRIBUTE);
	}

	
	public String getValue() {
		return xi.getStringValue();
	}

	
	public boolean isId() {
		return xi.getName()!=null && "id".equals(xi.getName().getLocalName());
	}

	
	public void setValue(String value) throws DOMException {
		xi.setStringValue(value);		
	}	

	
	public void appendData(String s) throws DOMException {
		String data = xi.getStringValue();
		if (data==null)
			data = "";
		if (s!=null)
			xi.setStringValue(data+s);		
	}

	
	public void deleteData(int i, int j) throws DOMException {
		String data = xi.getStringValue();
		if (data!=null) {
			StringBuilder sb = new StringBuilder(data);
			sb.delete(i, j);
			xi.setStringValue(sb.toString());
		}		
	}

	
	public String getData() throws DOMException {
		return xi.getStringValue();
	}

	
	public int getLength() {
		String s = xi.getStringValue();
		return s!=null ? s.length() : 0;
	}

	
	public void insertData(int i, String s) throws DOMException {
		String data = xi.getStringValue();
		StringBuilder sb = null;
		if (data!=null) {
			 sb = new StringBuilder(data);
		}
		else {
			sb = new StringBuilder();
		}
		sb.insert(i, s);
		xi.setStringValue(sb.toString());		
	}

	
	public void replaceData(int i, int j, String s) throws DOMException {
		String data = xi.getStringValue();
		StringBuilder sb = null;
		if (data!=null) {
			 sb = new StringBuilder(data);
		}
		else {
			sb = new StringBuilder();
		}
		sb.replace(i, j, s);
		xi.setStringValue(sb.toString());		
	}

	
	public void setData(String s) throws DOMException {
		xi.setStringValue(s);		
	}

	
	public String substringData(int i, int j) throws DOMException {
		String data = xi.getStringValue();
		if (data!=null && j <= data.length())
			return data.substring(i, j);
		return null;		
	}
	
	
	public String getWholeText() {
		return xi.getStringValue();
	}

	
	public boolean isElementContentWhitespace() {
		String data = xi.getStringValue();
		return data == null || data.trim().length() == 0;
	}

	
	public Text replaceWholeText(String s) throws DOMException {
		xi.setStringValue(s);
		return this;
	}

	
	public Text splitText(int i) throws DOMException {
		String data = xi.getStringValue();
		if (data == null || i >= data.length())
			return this;
		XiNode newNode = xi.copy();
		newNode.setStringValue(data.substring(0, i));
		xi.setStringValue(data.substring(i+1, data.length()));
		xi.getParentNode().insertBefore(newNode, xi);
		return this;		
	}

	
	public String getTarget() {
		return xi.getStringValue();
	}
	
	
	public Node adoptNode(Node node) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	
	public Attr createAttribute(String s) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	
	public Attr createAttributeNS(String s, String s1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	
	public CDATASection createCDATASection(String s) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	
	public Comment createComment(String s) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	
	public DocumentFragment createDocumentFragment() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	
	public Element createElement(String s) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	
	public Element createElementNS(String s, String s1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	
	public EntityReference createEntityReference(String s) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	
	public ProcessingInstruction createProcessingInstruction(String s, String s1)
			throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	
	public Text createTextNode(String s) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	
	public DocumentType getDoctype() {
		return null;
	}	
	
	public Element getDocumentElement() {
		return (XiNodeWrapper)getOwnerDocument().getFirstChild();
	}
	
	public String getDocumentURI() {
		return referenceNode.getOwnerDocument().getDocumentURI();
	}

	
	public DOMConfiguration getDomConfig() {
		return referenceNode.getOwnerDocument().getDomConfig();
	}

	
	public Element getElementById(String s) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	
	public DOMImplementation getImplementation() {
		return referenceNode.getOwnerDocument().getImplementation();
	}

	
	public String getInputEncoding() {
		return referenceNode.getOwnerDocument().getInputEncoding();
	}

	
	public boolean getStrictErrorChecking() {
		return referenceNode.getOwnerDocument().getStrictErrorChecking();
	}

	
	public String getXmlEncoding() {
		return referenceNode.getOwnerDocument().getXmlEncoding();
	}

	
	public boolean getXmlStandalone() {
		return referenceNode.getOwnerDocument().getXmlStandalone();
	}

	
	public String getXmlVersion() {
		return referenceNode.getOwnerDocument().getXmlVersion();
	}

	
	public Node importNode(Node node, boolean flag) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	
	public void normalizeDocument() {}

	
	public Node renameNode(Node node, String s, String s1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");
	}

	
	public void setDocumentURI(String s) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported by node wrapper");		
	}

	
	public void setStrictErrorChecking(boolean flag) {		
	}

	
	public void setXmlStandalone(boolean flag) throws DOMException {		
	}

	
	public void setXmlVersion(String s) throws DOMException {		
	}

	
	public NamedNodeMap getEntities() {
		return EMPTY_NAMED_NODE_MAP;
	}

	
	public String getInternalSubset() {
		return null;
	}

	
	public NamedNodeMap getNotations() {
		return EMPTY_NAMED_NODE_MAP;
	}

	
	public String getPublicId() {
		return "";
	}	
	
	public String getSystemId() {
		return "";
	}
	
	public final static void main(String[] args) throws Exception {
		PEProc.setRuntimeProperty("TSI_TRANSPORT", "tdi.transport.VoidTransport");
		XiNode doc = XiParserFactory.newInstance().parse(new InputSource(new StringReader("<?xml version=\"1.0\"?><!-- comment --><ns1:root xmlns:ns1=\"http://www.example.cpm\"><ns1:a><![CDATA[aaaa]]></ns1:a><ns1:b>gżegżółka</ns1:b></ns1:root>")));
		XPathMarkerDefinition md = new XPathMarkerDefinition("1", "/root/a");
		if (md.matches(doc.getFirstChild())) {
			for (String s : md.getMatches())
				System.out.println(s);
		}
	}
	
	public String toString() {
		return "["+xi.getNodeKind().toString()+" "+getTagName()+" : "+xi.getStringValue()+"]";		
	}	
}