package com.tibco.xml.channel.infoset.helpers;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.tibco.xml.channel.infoset.XmlContentFilter;
import com.tibco.xml.channel.infoset.XmlContentHandler;
import com.tibco.xml.data.primitive.ExpandedName;
import com.tibco.xml.data.primitive.XmlTypedValue;
import com.tibco.xml.schema.SmAttribute;
import com.tibco.xml.schema.SmElement;
import com.tibco.xml.schema.SmType;

public class XmlContentTracer implements XmlContentFilter {
	
	private static class Entry {
		public long start = 0;
		public long end = 0;
		public long nStart = 0;
		public long nEnd = 0;
		public String entity = "";
		
		public long durationMillis() {
			return end-start;
		}
		public long durationNanos() {
			long d = nEnd - nStart;
			if (d >= 0 && d/1000000 <= (end-start)+1)
				return d;
			return 0;
		}
	}
	
	private XmlContentHandler handler;
	
	private int nestCount = 0;
	private Stack<Entry> stack = new Stack<Entry>();
	private TreeMap<String,List<Entry>> aggregateList = new TreeMap<String,List<Entry>>();
	private static HashMap<Long, String> results = new HashMap<Long, String>();
	private Long myThread = 0L;
	
	public XmlContentTracer() {}
	
	public XmlContentTracer(PrintStream printstream) {}
	
	public XmlContentTracer(XmlContentHandler xmlcontenthandler)
	{
	    handler = xmlcontenthandler;
	}
	
	public XmlContentTracer(XmlContentHandler xmlcontenthandler, PrintStream printstream)
	{
	    this(xmlcontenthandler);
	}
	
	@Override
	protected void finalize() throws Throwable {
		results.remove(myThread);
		super.finalize();
	}
	
	public void setDocumentLocator(Locator locator) {
	    handler.setDocumentLocator(locator);
	}
	
	public void startDocument() throws SAXException	{
		handler.startDocument();
		nestCount = 0;
		stack.clear();
		myThread = Thread.currentThread().getId();
	}
	
	public void endDocument() throws SAXException {
	    handler.endDocument();
	    nestCount = 0;
		stack.clear();
		results.put(Thread.currentThread().getId(), getProfilerResult());		
	}
	
	public static String popResultForThread(long tid) {
		return results.remove(tid);
	}
	
	private String getProfilerResult() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String,List<Entry>> en : aggregateList.entrySet()) {
			double ms = 0;
			double ns = 0;
			List<Entry> list = en.getValue();
			for (Entry ent : list) {
				ms += ent.durationMillis();
				ns += ent.durationNanos();
			}
			ms /= list.size();
			ns /= list.size();
			if (ms > 0) {
				sb.append(en.getKey()).append("(").append(
					String.format("%.3f%n", ms).replace(',','.')).append(" ms")
					.append(" x").append(list.size()).append("), ");
			}
			else if (ns/1000 > 0){
				sb.append(en.getKey()).append("(").append(
					String.format("%.3f%n", ns/1000).replace(',','.')).append(" us")
					.append(" x").append(list.size()).append("), ");
			}
			else {
				sb.append(en.getKey()).append("(").append(
					String.format("%.3f%n", ns).replace(',','.')).append(" ns")
					.append("x").append(list.size()).append("), ");
			}				
		}
		aggregateList.clear();
		return sb.toString();
	}

	public void prefixMapping(String pfx, String uri) throws SAXException {
	    handler.prefixMapping(pfx, uri);
	}
	
	public void startElement(ExpandedName expandedname, SmElement smelement, SmType smtype)
	    throws SAXException
	{
	    try {
	        handler.startElement(expandedname, smelement, smtype);
	        nestCount++;
	        Entry e = new Entry();
	        e.start = System.currentTimeMillis();
	        e.nStart = System.nanoTime();
	        e.entity = expandedname.getLocalName();
	        stack.push(e);	        
	    }
	    catch(SAXException sx) {        
	        throw sx;
	    }
	    catch(RuntimeException re) {
	        throw re;
	    }
	}
	
	public void endElement(ExpandedName expandedname, SmElement smelement, SmType smtype)
	    throws SAXException
	{
		handler.endElement(expandedname, smelement, smtype);
		Entry e = stack.pop();
		if (e!=null && e.entity.equals(expandedname.getLocalName())) {
			e.end = System.currentTimeMillis();
			e.nEnd = System.nanoTime();
			String key = nestCount+"|"+expandedname.getLocalName();
			List<Entry> list = aggregateList.get(key);
			if (list==null) {
				list = new LinkedList<Entry>();
				aggregateList.put(key, list);
			}
			list.add(e);
			nestCount--;
		}		
	}
	
	public void attribute(ExpandedName expandedname, String s, SmAttribute smattribute)
	    throws SAXException
	{
	    handler.attribute(expandedname, s, smattribute);
	}
	
	public void attribute(ExpandedName expandedname, XmlTypedValue xmltypedvalue, SmAttribute smattribute)
	    throws SAXException
	{
	    handler.attribute(expandedname, xmltypedvalue, smattribute);
	}
	
	public void text(String s, boolean flag) throws SAXException {
		try {
	    	handler.text(s, flag);
	    }
	    catch(SAXException sx) {
	        throw sx;
	    }
	    catch(RuntimeException re) {
	        throw re;
	    }
	}
	
	public void text(XmlTypedValue xmltypedvalue, boolean flag) throws SAXException	{
	    try {
	    	handler.text(xmltypedvalue, flag);
	    }
	    catch(SAXException sx) {
	        throw sx;
	    }
	    catch(RuntimeException re) {
	        throw re;
	    }
	}
	
	public void ignorableWhitespace(String value, boolean flag) throws SAXException {
	    handler.ignorableWhitespace(value, flag);
	}
	
	public void processingInstruction(String target, String data) throws SAXException {
		handler.processingInstruction(target, data);
	}
	
	public void comment(String s) throws SAXException {
	    handler.comment(s);
	}
	
	public void skippedEntity(String s) throws SAXException	{
	    handler.skippedEntity(s);
	}
	
	public void setXmlContentHandler(XmlContentHandler xmlcontenthandler) {
	    handler = xmlcontenthandler;   
	}
	
	
}
