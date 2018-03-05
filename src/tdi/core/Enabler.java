package tdi.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

/*
 * Copyright (c) 2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

public class Enabler {
	
	private final static String FROM = "java.start.class=com.tibco.pe.PEMain";
	private final static String TO =   "java.start.class=tdi.com.tibco.pe.PE";
	private final static String BW_HOME_PROP = "tibco.env.BW_HOME";
	
	public static boolean enableTdiInTra(File tra) throws Exception {
		RandomAccessFile fis = new RandomAccessFile(tra, "rw");
		FileChannel channel = fis.getChannel();
		MappedByteBuffer buff = channel.map(MapMode.READ_WRITE, 0, tra.length());
		int ptr = find(buff, BW_HOME_PROP, (int) tra.length());
		if (ptr!=-1) {
			buff.position(ptr + BW_HOME_PROP.length());
			StringBuffer sb = new StringBuffer();
			byte c = buff.get();
			do {
				c = buff.get();
				sb.append((char)c);
			}
			while (c!='\r' && c!='\n' && c!='#');
			String bwPath = sb.toString().trim();
			System.out.println("BW_PATH="+bwPath);
			try {
				String mineUrl = Enabler.class.getResource("/"+Enabler.class.getName().replace('.', '/')+".class").getPath();
				if (mineUrl.startsWith("file://"))
					mineUrl = mineUrl.substring(7, mineUrl.lastIndexOf('!'));
				else if (mineUrl.startsWith("file:/"))
					mineUrl = mineUrl.substring(6, mineUrl.lastIndexOf('!'));
				else
					mineUrl = mineUrl.substring(0, mineUrl.lastIndexOf('!'));
				System.out.println("MY_HOME="+mineUrl);
				Files_copy(mineUrl, bwPath+"/hotfix/lib/"+new File(mineUrl).getName());	
			}
			catch (Exception e) {
				System.err.println("Cannot copy files: "+e.toString());
			}
		}
		buff.position(0);
		
		int pos = find(buff, FROM, (int) tra.length());
		if (pos!=-1) {
			write(buff, TO, pos);
		}
		else {
			buff.position(0);
			pos = find(buff, TO, (int) tra.length());
		}
		channel.force(true);
		channel.close();
		fis.close();
		return pos!=-1;
	}

	private static void Files_copy(String from, String to) throws Exception {
		if (from.charAt(1)=='\\')
			from = from.charAt(0)+from.substring(2);
		if (to.charAt(1)=='\\')
			to = to.charAt(0)+to.substring(2);
		System.out.println(from + " => " + to);
		FileInputStream fis = new FileInputStream(from);
		try {
			FileOutputStream fos = new FileOutputStream(to);
			byte[] buff = new byte[4096];
			int n = 0;
			while ( (n = fis.read(buff)) > -1 ) {
				fos.write(buff, 0, n);
			}
			fos.close();
		}
		finally {
			fis.close();
		}
	}

	private static void write(MappedByteBuffer buff, String s, int pos) {
		buff.position(pos);
		buff.put(s.getBytes());
		buff.force();
	}

	private static int find(MappedByteBuffer buff, String search, int len) {
		byte[] bsearch = search.getBytes();
		int match = -1;
		int cnt = 0;
		buff.position(0);
		while (buff.position() < len) {
			byte b = buff.get();
			if (b == bsearch[cnt]) {
				if (cnt==0)
					match = buff.position()-1;
				if (++cnt == bsearch.length) {					
					return match;
				}
			}
			else				
				cnt = 0;			
		}		
		return -1;
	}
	
	public final static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {}
		try {
			JFileChooser jfc = new JFileChooser();
			String lastPath = loadLastPath();
			if (lastPath!=null)
				jfc.setSelectedFile(new File(lastPath));
			jfc.setFileFilter(new FileFilter() {
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().toLowerCase().endsWith(".tra"); 
				}

				@Override
				public String getDescription() {
					return "TRA file";
				}				
			});
			File traFile = null;
			if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
				traFile = jfc.getSelectedFile();
			if (traFile == null) {
				JOptionPane.showMessageDialog(jfc, "Please select .tra file", "TDI Enabler", JOptionPane.WARNING_MESSAGE);
				return;
			}
			else {
				saveLastPath(traFile.getParentFile());
				boolean result = false;
				try {
					result = enableTdiInTra(traFile);	
					JOptionPane.showMessageDialog(jfc, traFile.getAbsolutePath(),
							result ? "File was modified" : "File was NOT modified",
							result ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
				}
				catch (Exception exc) {
					exc.printStackTrace();
					JOptionPane.showMessageDialog(jfc, exc.toString(), "Error while processing tra file", JOptionPane.ERROR_MESSAGE);
				}
				return;			
			}
		}
		catch (Exception exc) {}
		
		if (args.length==0) {
			System.out.println("Usage: path-to-tra-file");
		}
		else {
			enableTdiInTra(new File(args[0]));
		}		
	}

	private static String loadLastPath() {
		try {
			Properties p = new Properties();
			FileInputStream fis = new FileInputStream(".tdi_enabler");
			p.loadFromXML(fis);
			fis.close();
			Object val = p.getProperty("lastPath");
			return val!=null ? val.toString() : null;
		}
		catch (Exception e) {}
		return null;
	}
	
	private static void saveLastPath(File traFile) throws Exception {
		Properties p = new Properties();
		p.put("lastPath", traFile.getAbsolutePath());
		FileOutputStream fos = new FileOutputStream(".tdi_enabler");
		p.storeToXML(fos, "config");
		fos.close();		
	}
}
