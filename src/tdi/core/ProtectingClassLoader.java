/*
 * Copyright (c) 2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.util.HashMap;

public class ProtectingClassLoader extends URLClassLoader {
	
	private final static HashMap<String,Class<?>> classCache = new HashMap<String,Class<?>>();
	private static ProtectingClassLoader INSTANCE = null;
	
	public static ProtectingClassLoader getInstance() {
		synchronized (classCache) {
			if (INSTANCE==null)
				INSTANCE = new ProtectingClassLoader();
		}
		return INSTANCE;
	}
	
	public static String getJarName() {
		String jarName = null;
		try {
			URL jarUrl = ProtectingClassLoader.class.getResource("/"+ProtectingClassLoader.class.getName().replace('.', '/')+".class");
			if (jarUrl!=null) {
				jarName = jarUrl.getPath();
				int i = jarName.lastIndexOf("!");
				if (i>0)
					jarName = jarName.substring(0, i);
				if (jarName.startsWith("file:"))
					jarName = jarName.substring(System.getProperty("os.name", "?").contains("Win") ? 6 : 5);
			}
		}
		catch (Exception e) {}
		return jarName;
	}
	
	public static abstract class Closure {
		public abstract Object invoke() throws Exception;
	}
	
	public static URL getJarUrl() {
		try {
			return new URL("jar:file://"+getJarName()+"!/");
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public ProtectingClassLoader(URL mainJar) {
		super(mainJar == null ? new URL[0] : new URL[] { mainJar }, Thread.currentThread().getContextClassLoader());
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (cl instanceof URLClassLoader) {
			for (URL url : ((URLClassLoader) cl).getURLs()) {
				this.addURL(url);
			}			
		}
		Thread.currentThread().setContextClassLoader(this);
	}
	
	public ProtectingClassLoader() {
		this(getJarUrl());				
	}
	
	public final static Object seal(Closure r) throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(new ProtectingClassLoader());
			return r.invoke();
		}
		finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
	}
	private final static byte[] key = loadKey();
	
	private final static boolean isProtectedClass(String s) {
		return s!=null && (s.startsWith("tdi.") || s.startsWith("tdi/")) &&
			!s.contains("ProtectingClassLoader");
	}
	
	private static byte[] loadKey() {
		InputStream is = ProtectingClassLoader.class.getResourceAsStream("/license.txt");
		if (is==null)
			throw new RuntimeException("No needed resource found");
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] b = new byte[4096];
			while (true) {
				int cnt = is.read(b);
				if (cnt >= 0)
					baos.write(b, 0, cnt);
				else
					break;
			}
			baos.flush();
			byte[] buff = baos.toByteArray();
			baos.close();
			return MessageDigest.getInstance("SHA-256").digest(buff);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			if (is!=null) {
				try {
					is.close();
				}
				catch (IOException e) {}
			}
		}
	}

	@Override
	public Class<?> loadClass(String s) throws ClassNotFoundException {
		if (isProtectedClass(s))
			return loadProtectedClass(s);
		return super.loadClass(s);
	}

	private Class<?> loadProtectedClass(String s) throws ClassNotFoundException {
		Class<?> fromCache = classCache.get(s);
		if (fromCache!=null)
			return fromCache;
		InputStream is = ProtectingClassLoader.class.getResourceAsStream("/"+s.replace('.', '/')+".class");
		if (is==null)
			throw new ClassNotFoundException(s+".Protected");
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] b = new byte[32768];
			while (true) {
				int cnt = is.read(b);
				if (cnt >= 0)
					baos.write(b, 0, cnt);
				else
					break;
			}
			baos.flush();
			byte[] buff = baos.toByteArray();			
			boolean magicOK = ((buff[0] & 0xff)== 0xca) && ((buff[1] & 0xff)== 0xfe) && 
				((buff[2] & 0xff)== 0xba) && ((buff[3] & 0xff)== 0xbe);
			
			if (!magicOK) {
				for (int i=0; i < buff.length; i++) {
					buff[i] = (byte) ((buff[i] ^ key[i % key.length]) & 0xff);
				}
			}
			Class<?> defined = defineClass(s, buff, 0, buff.length);
			resolveClass(defined);
			classCache.put(s, defined);
			return defined;
		}
		catch (Exception ex) {
			throw new ClassNotFoundException(s+".Defined", ex);			
		}
		finally {
			if (is!=null) {
				try {
					is.close();
				}
				catch (IOException e) {}
			}
		}		
	}

	@Override
	protected Class<?> loadClass(String s, boolean flag)
			throws ClassNotFoundException {
		if (isProtectedClass(s))
			return loadProtectedClass(s);
		return super.loadClass(s, flag);
	}

	@Override
	protected Class<?> findClass(String s) throws ClassNotFoundException {
		if (isProtectedClass(s))
			return loadProtectedClass(s);
		return super.findClass(s);
	}
	
	protected RandomAccessFile raf(File fPath) throws FileNotFoundException {
		return new RandomAccessFile(fPath, "rw");
	}
	
	private static void processFileEntry(File fPath) throws Exception {
		if (fPath.isDirectory()) {
			for (File f : fPath.listFiles())
				processFileEntry(f);
		}
		else if (fPath.getName().endsWith(".class")) {
			if (fPath.getName().endsWith("PE.class") ||
				fPath.getName().endsWith("BWLoader.class") ||
				fPath.getName().contains("Enabler.class") ||
				fPath.getName().contains("Enabler$") ||
				fPath.getName().contains("HSQLServerLoader") ||
				fPath.getName().contains("palettes") ||
				fPath.getName().contains("apiHome") ||
				fPath.getName().contains("ProtectingClassLoader")) {
				System.out.println("Skipping "+fPath.getName());
				return;
			}
			System.out.println("Processing "+fPath.getName());
			RandomAccessFile raf = new RandomAccessFile(fPath, "rw");
			byte buff[] = new byte[4];
			raf.read(buff);			
			int pos = 0;
			while (pos < fPath.length()) {
				raf.seek(pos);
				byte b = raf.readByte();
				raf.seek(pos);
				raf.writeByte( b ^ key[pos % key.length] );
				pos++;
			}
			raf.close();
		}		
	}
	
	public final static void main(String[] args) throws Exception {
		if (args.length==0) {
			String path = "C:\\Users\\jakub.jozwicki\\workspace\\TDI\\binn";
			File fPath = new File(path);
			processFileEntry(fPath);
		}		
	} 
}
