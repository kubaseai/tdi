package com.tibco.xml.channel.infoset;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import tdi.core.CompileTimeStub;

import com.tibco.xml.data.primitive.ExpandedName;
import com.tibco.xml.data.primitive.XmlTypedValue;
import com.tibco.xml.schema.SmAttribute;
import com.tibco.xml.schema.SmElement;
import com.tibco.xml.schema.SmType;

@CompileTimeStub
public interface XmlContentHandler
{

    public abstract void setDocumentLocator(Locator locator);

    public abstract void startDocument()
        throws SAXException;

    public abstract void endDocument()
        throws SAXException;

    public abstract void startElement(ExpandedName expandedname, SmElement smelement, SmType smtype)
        throws SAXException;

    public abstract void endElement(ExpandedName expandedname, SmElement smelement, SmType smtype)
        throws SAXException;

    public abstract void attribute(ExpandedName expandedname, String s, SmAttribute smattribute)
        throws SAXException;

    public abstract void attribute(ExpandedName expandedname, XmlTypedValue xmltypedvalue, SmAttribute smattribute)
        throws SAXException;

    public abstract void text(String s, boolean flag)
        throws SAXException;

    public abstract void text(XmlTypedValue xmltypedvalue, boolean flag)
        throws SAXException;

    public abstract void ignorableWhitespace(String s, boolean flag)
        throws SAXException;

    public abstract void processingInstruction(String s, String s1)
        throws SAXException;

    public abstract void comment(String s)
        throws SAXException;

    public abstract void prefixMapping(String s, String s1)
        throws SAXException;

    public abstract void skippedEntity(String s)
        throws SAXException;
}
