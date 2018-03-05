package tdi.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import org.xml.sax.InputSource;

import com.tibco.pe.core.TrackData;
import com.tibco.xml.datamodel.XiParser;
import com.tibco.xml.datamodel.XiParserFactory;
import com.tibco.xml.xdata.xpath.Variable;
import com.tibco.xml.xdata.xpath.VariableList;

public class JobExtractor {

	public String group;
	public String name;
	public String starterName;
	public long jid = 0;
	public String task;
	public String wf;
	public int version = 0;
	public int procTime = 0;
	public boolean dead = false;
	public HashMap<String, String> attributes;
	public Object restartContext;
	public int spawnCnt = 0;
	public String dupKey;
	public List<TrackDataExtractor> trackDataList;
	public Hashtable<Object,Object> claimStore;
	public int nextClaimCheck;
	public String customId;
	public Object sequenceKey;
	public long timestamp;
	public HashMap<Object,Object> processInstanceVars;
	public String service;
	public HashMap<Object,Object> transitionStates;
	public boolean ishibernated;
	
	public static Object deserialize(Object obj, byte[] bytes) throws Exception {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
		Method wo = obj.getClass().getDeclaredMethod("readObject",
				new Class[] { ObjectInputStream.class });
		wo.setAccessible(true);
		wo.invoke(obj, ois);
		return obj;
	}

	public static byte[] serialize(Object obj) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		Method wo = obj.getClass().getDeclaredMethod("writeObject",
				new Class[] { ObjectOutputStream.class });
		wo.setAccessible(true);
		wo.invoke(obj, oos);
		oos.flush();
		baos.flush();
		return baos.toByteArray();
	}

	public static class TrackDataExtractor {
		public Stack<Object> errorHandlers;
		public String concurrencyId;
		public boolean blocked;
		public String transitionDestTaskName;
		public String name;
		public int id = -1;
		public int parentId;
		public String workflow;
		public String callStack;
		public HashMap<String, String> attributes;

		@SuppressWarnings("unchecked")
		private void readSerialized(ObjectInputStream stream) throws Exception {
			name = (String) stream.readObject();
			transitionDestTaskName = (String) stream.readObject();
			id = stream.readInt();
			parentId = stream.readInt();
			workflow = (String) stream.readObject();
			callStack = (String) stream.readObject();
			attributes = attributesVarListToMap((VariableList) stream
					.readObject());
			blocked = stream.readBoolean();
			try {
				concurrencyId = (String) stream.readObject();
			} catch (Exception e) {
			}
			try {
				errorHandlers = (Stack<Object>) stream.readObject();
			} catch (Exception e) {
			}
		}
		
		private void writeSerialized(ObjectOutputStream stream) throws Exception {
			stream.writeObject(name);
			stream.writeObject(transitionDestTaskName);
			stream.writeInt(id);
			stream.writeInt(parentId);
			stream.writeObject(workflow);
			stream.writeObject(callStack);
			stream.writeObject(attributesMapToVarList(attributes));
			stream.writeBoolean(blocked);
			stream.writeObject(concurrencyId);
			stream.writeObject(errorHandlers);			
		}

		protected static List<TrackDataExtractor> extractTrackData(
				List<TrackData> input) throws Exception {
			LinkedList<TrackDataExtractor> list = new LinkedList<TrackDataExtractor>();
			if (input!=null) {
				for (TrackData td : input) {
					TrackDataExtractor tde = new TrackDataExtractor();
					tde.readSerialized(new ObjectInputStream(
							new ByteArrayInputStream(serialize(td))));
					list.add(tde);
				}
			}
			return list;
		}

		public static List<TrackData> exportTrackData(
				List<TrackDataExtractor> trackDataList) throws Exception {
			List<TrackData> list = new LinkedList<TrackData>();
			for (TrackDataExtractor tde : trackDataList) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				tde.writeSerialized(oos);
				TrackData td = TrackData.class.newInstance();
				deserialize(td, baos.toByteArray());
				list.add(td);
			}
			return list;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void readSerialized(ObjectInputStream stream) throws Exception {
		group = (String) stream.readObject();
		name = (String) stream.readObject();
		starterName = (String) stream.readObject();
		jid = stream.readLong();
		task = (String) stream.readObject();
		wf = (String) stream.readObject();
		version = stream.readInt();
		procTime = stream.readInt();
		dead = stream.readBoolean();
		attributes = attributesVarListToMap((VariableList) stream.readObject());
		restartContext = stream.readObject();
		spawnCnt = stream.readInt();
		dupKey = (String) stream.readObject();
		trackDataList = TrackDataExtractor
				.extractTrackData((List<TrackData>) stream.readObject());
		try {
			claimStore = (Hashtable) stream.readObject();
		}
		catch (Exception e) {
		}
		try {
			nextClaimCheck = stream.readInt();
		}
		catch (Exception e) {
		}
		try {
			customId = (String) stream.readObject();
		}
		catch (Exception e) {
		}
		try {
			sequenceKey = stream.readObject();
		}
		catch (Exception e) {
		}
		try {
			timestamp = stream.readLong();
		}
		catch (Exception e) {
		}
		try {
			processInstanceVars = (HashMap) stream.readObject();
		}
		catch (Exception e) {
		}
		try {
			service = (String) stream.readObject();
		}
		catch (Exception e) {
		}
		try {
			transitionStates = (HashMap) stream.readObject();
		}
		catch (Exception e) {
		}
		try {
			ishibernated = stream.readBoolean();
		}
		catch (Exception e) {
		}
	}
	
	public void writeSerialized(ObjectOutputStream stream) throws Exception {
		stream.writeObject(group);
		stream.writeObject(name);
		stream.writeObject(starterName);
		stream.writeLong(jid);
		stream.writeObject(task);
		stream.writeObject(wf);
		stream.writeInt(version);
		stream.writeInt(procTime);
		stream.writeBoolean(dead);
		stream.writeObject(attributesMapToVarList(attributes));
		stream.writeObject(restartContext);
		stream.writeInt(spawnCnt);
		stream.writeObject(dupKey);
		List<TrackData> trackDataList_ = TrackDataExtractor
				.exportTrackData(trackDataList);
		stream.writeObject(trackDataList_);
		stream.writeObject(claimStore);
		stream.writeInt(nextClaimCheck);
		stream.writeObject(customId);
		stream.writeObject(sequenceKey);
		stream.writeLong(timestamp);
		stream.writeObject(processInstanceVars);
		stream.writeObject(service);
		stream.writeObject(transitionStates);
		stream.writeBoolean(ishibernated);		
	}

	private static HashMap<String, String> attributesVarListToMap(
			VariableList attributesList) throws Exception {
		HashMap<String, String> attributes = new HashMap<String, String>();
		if (attributesList != null) {
			for (Object aName : attributesList.getVariableNames()) {
				Variable aValue = attributesList.getVariable(aName.toString());
				String value = null;
				if (aValue != null) {
					ObjectInputStream ois = new ObjectInputStream(
						new ByteArrayInputStream(serialize(aValue)));
					Object content = ois.readObject();
					if (content instanceof String)
						value = (String) content;
					else
						value = new String((byte[])content);
				}
				attributes.put(aName.toString(), value);
			}
		}
		return attributes;
	}
	
	private static VariableList attributesMapToVarList(HashMap<String, String> a) throws Exception {
		XiParser p = XiParserFactory.newInstance();
		VariableList vl = new VariableList(a.keySet().toArray(new String[0]));
		for (Entry<String,String> en : a.entrySet()) {
			String value = en.getValue();
			if (value!=null && value.contains("xml")) {
				vl.setVariable(en.getKey(), new Variable(p.parse(new InputSource(new StringReader(value)))));
			}
			else if (value!=null)
				vl.setVariable(en.getKey(), new Variable(Double.valueOf(value.substring(1))));
		}
		return vl;
	}
}
