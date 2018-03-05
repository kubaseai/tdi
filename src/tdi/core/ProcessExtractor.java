package tdi.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.tibco.bw.store.RepoAgent;
import com.tibco.objectrepo.vfile.VFile;
import com.tibco.objectrepo.vfile.VFileDirectory;
import com.tibco.objectrepo.vfile.VFileFactory;
import com.tibco.objectrepo.vfile.VFileStream;
import com.tibco.pe.core.Engine;
import com.tibco.pe.load.ProcessModelObjectFactory;
import com.tibco.pe.load.ProcessModelXiNodeObjectFactory;
import com.tibco.pe.model.ProcessModel;
import com.tibco.xml.datamodel.XiNode;


public class ProcessExtractor {
	
	private static final String URI_PREFIX = "imp:///";

	protected static LinkedList<VFileStream> retrieveVFiles(String type, VFileDirectory dir) {
		LinkedList<VFileStream> list = new LinkedList<VFileStream>();
		if (null == dir || null == type)
			return list;
		try {
			Iterator<VFile> iterator = dir.getChildren();
			do {
				if (!iterator.hasNext())
					break;
				VFile vfile = (VFile) iterator.next();
				if (vfile instanceof VFileDirectory)
					list.addAll(retrieveVFiles(type, (VFileDirectory) vfile));
				else if (type.equals(((VFileStream) vfile).getType()))
					list.add((VFileStream) vfile);
			}
			while (true);
		}
		catch (Exception objectrepoexception) {}
		return list;
	}
	
	public static HashMap<String,String[]> retrieveFilesByExtension(RepoAgent ra, String type,
		boolean computeHash) throws NoSuchAlgorithmException
	{
		MessageDigest md = MessageDigest.getInstance("MD5");
		HashMap<String,String[]> list = new HashMap<String,String[]>();
		Iterator<VFileFactory> projects = ra.getObjectProvider().getAllProjects();
		byte buff[] = new byte[4096];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (projects.hasNext()) {
			for (VFileStream vfs : retrieveVFiles(type, projects.next().getRootDirectory())) {
				InputStream is = null;
				baos.reset();
				try {
					is = vfs.getInputStream();
					int n = 0;
					do {
						n = is.read(buff);
						if (n > 0)
							baos.write(buff, 0, n);
					}
					while (n>=0);
				}
				catch (Exception e) {}
				finally {
					if (is!=null) {
						try {
							is.close();
						}
						catch (Exception exc) {}
					}					
				}
				md.reset();
				byte[] content = baos.toByteArray();
				StringBuilder hash = new StringBuilder();
				if (computeHash) {
					for (byte b : md.digest(content))
					 hash.append( String.format("%02x", b) );
				}
				String uri = vfs.getFullURI();
				if (uri!=null && uri.startsWith(URI_PREFIX))
					uri = uri.substring(URI_PREFIX.length());
				list.put(uri, new String[] { new String(content), hash.toString() });				
			}			
		}
		return list;
	}
	
	public static InputStream retrieveFileAsStream(RepoAgent ra, String name) throws NoSuchAlgorithmException {
		Iterator<VFileFactory> projects = ra.getObjectProvider().getAllProjects();
		while (name!=null && name.length() > 0 && projects.hasNext()) {
			VFileDirectory root = projects.next().getRootDirectory();
			InputStream is = scanDir(ra, name, root, root.getFullURI().length());
			if (is!=null)
				return is;
		}
		return new ByteArrayInputStream(new byte[0]);
	}

	private static InputStream scanDir(RepoAgent ra, String name,
			VFileDirectory root, int pfxLen) {
		Iterator<VFile> it = root.getChildren();
		while (it.hasNext()) {
			VFile vf = it.next();
			VFileStream vfs = vf instanceof VFileStream ? (VFileStream) vf : null;
			if (vfs!=null && vfs.getFullURI().length() > pfxLen && 
				vfs.getFullURI().substring(pfxLen).replace('\\', '/').equals(name.replace('\\','/'))) {
				return vfs.getInputStream();
			}
			if (vf instanceof VFileDirectory) {
				InputStream is = scanDir(ra, name, (VFileDirectory)vf, pfxLen);
				if (is!=null)
					return is;
			}
		}
		return null;
	}

	public static HashMap<String,String[]> retrieveProcessFiles() {
		try {
			return retrieveFilesByExtension(Engine.getRepoAgent(), "process", true);
		}
		catch (NoSuchAlgorithmException nsae) {
			return new HashMap<String, String[]>();
		}
	}
	
	public static HashMap<String,String[]> retrieveFilesByExtension(String[] types,
			boolean computeHash) {
		HashMap<String, String[]> map = new HashMap<String, String[]>();
		for (String t : types) {
			try {
				map.putAll( retrieveFilesByExtension(Engine.getRepoAgent(), t, computeHash) );
			}
			catch (NoSuchAlgorithmException nsae) {}
		}
		return map;
	}
	
	public static ProcessModel retrieveProcessModel(String name) throws NoSuchAlgorithmException, IOException {
		ProcessModelXiNodeObjectFactory f = new ProcessModelXiNodeObjectFactory() {					
			ProcessModelObjectFactory pmof = new ProcessModelObjectFactory();				
			@Override
			public XiNode getNodeForObject(Object obj) {
				return pmof.getNodeForObject(obj);
			}					
			@Override
			public Object createObjectForData(XiNode xinode) {
				return pmof.createObjectForData(xinode);
			}
		};
		InputStream processStream = ProcessExtractor.
			retrieveFileAsStream(Engine.getRepoAgent(), name);
		ProcessModel pm = null;
		if (processStream!=null)
			pm = (ProcessModel) f.readFromStream(processStream);
		return pm;		
	}
	
	public static String retrieveFileContent(String name) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buff = new byte[4096];
			InputStream is = retrieveFileAsStream(Engine.getRepoAgent(), name);
			if (is!=null) {
				int n = 0;
				do {
					n = is.read(buff);
					if (n>0)
						baos.write(buff, 0, n);
				}
				while (n>0);
				is.close();
				return baos.toString("UTF-8");
			}
		}
		catch (Exception ex) {}
		return null;		
	}
	
	public static void main(String[] args) throws Exception {
		for (Map.Entry<String, String[]> en : retrieveProcessFiles().entrySet()) {
			System.out.println(en.getKey() + " => " + en.getValue()[1]);
		}
	}
}
