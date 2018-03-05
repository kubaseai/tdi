/*
 * Copyright (c) 2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import org.hsqldb.util.DatabaseManagerSwing;

import tdi.core.ProtectingClassLoader;

public class HSQLServerGUI {
	
	private JFrame frame = null;
	private final static HashMap<String,HSQLServerGUI> guiCache = new HashMap<String, HSQLServerGUI>();
	private static boolean minimize = true;
	
	public final static HSQLServerGUI getForUrl(String url) {
		HSQLServerGUI gui = guiCache.get(url);
		if (gui==null) {
			gui = new HSQLServerGUI(url);
			guiCache.put(url, gui);
		}
		gui.frame.setVisible(true);
		return gui;
	}
	
	public HSQLServerGUI(String url) {
		try {
			init(url);
		}
		catch (Exception ex) {
			try {
				JOptionPane.showMessageDialog(null, ex.toString(), "Error connecting to TDI DB", JOptionPane.ERROR_MESSAGE);
				if (frame!=null) {
					frame.setVisible(false);
					frame.dispose();
				}
			}
			catch (Exception e) {}
		}
	}
	
	private void init(final String remoteAddress) throws Exception {
		frame = new JFrame("TDI database: tdi://"+remoteAddress+"; tdi.sql.JDBCDriver");
		frame.setVisible(false);
		frame.setSize(800, 600);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		if (minimize)
			frame.setExtendedState(JFrame.ICONIFIED);
		final Connection conn = (Connection) ProtectingClassLoader.seal(new ProtectingClassLoader.Closure() {			
			@Override
			public Object invoke() throws Exception {
				DriverManager.registerDriver(new tdi.sql.JDBCDriver());
				for (int i=0; i < 12; i++) {
					try {
						return DriverManager.getConnection("tdi:"+remoteAddress, "sa", "");						
					}
					catch (Exception e) {
						if (i==11)
							throw e;
						else
							Thread.sleep(5000);
					}
				}
				throw new Exception("Could not connect to DB");
			}
		});		
		DatabaseManagerSwing manager = new DatabaseManagerSwing(frame);
		manager.main();
		InvocationHandler handler = new InvocationHandler() {
			public Object invoke(Object obj, Method method, Object[] aobj)
					throws Throwable {
				Object result = method.invoke(conn, aobj);
				if (result instanceof Statement) {
					try {
						((Statement)result).setMaxRows(2048);
					}
					catch (Exception e) {}
				}
				return result;
			}			
		};
		Connection proxy = (Connection) Proxy.newProxyInstance(
			Connection.class.getClassLoader(), new Class[] { Connection.class },
            handler
        );
		manager.connect(proxy);
		setupLookAndFeel();
		frame.invalidate();
		frame.setVisible(true);	
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setupLookAndFeel();
			}
		});
	}
	
	private final static void setupLookAndFeel() {
		for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
	        if ("Nimbus".equals(info.getName())) {
	            try {
					UIManager.setLookAndFeel(info.getClassName());
				}
	            catch (Exception e) {}
	            break;
	        }	        
	    }
	}
	
	public final static void main(String[] args) {
		System.setProperty("awt.useSystemAAFontSettings", "lcd");
		System.setProperty("sun.java2d.opengl", "false");
		System.setProperty("swing.aatext", "true");
		setupLookAndFeel();
		String url = JOptionPane.showInputDialog(null, "Enter address of TDI native database (host:8192)", "localhost:8192");
		HSQLServerGUI.minimize = false;
		if (url!=null && url.length()>0)
			HSQLServerGUI.getForUrl(url);
	}
}
