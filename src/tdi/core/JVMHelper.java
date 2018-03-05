package tdi.core;

import java.util.LinkedList;

import javax.swing.JOptionPane;

public class JVMHelper {
	
	private static LinkedList<Runnable> hooks = new LinkedList<Runnable>();
	private final static LinkedList<byte[]> reservedMem = new LinkedList<byte[]>();
	
	static {
		reservedMem.add(new byte[1024*1024]);
	}

	public static Runnable addShutdownHook(Runnable hook) {
		hooks.add(hook);
		return hook;
	}
	
	public static void runHooks() {
		for (Runnable r : hooks) {
			if (r instanceof Thread) {
				((Thread) r).start();
			}
			else
				new Thread(r).start();
		}
	}
	
	public static void oomExit(OutOfMemoryError oom) {
		reservedMem.clear();
		System.gc();
		Logger.getInstance().debug("OutOfMemoryError - shuting down JVM, msg="+oom.toString());
		try {
			JOptionPane.showMessageDialog(null, "Process consumed all memory and needs to be closed",
				"Not enough memory", JOptionPane.ERROR_MESSAGE);
		}
		catch (Exception e) {}
		runHooks();
		Runtime.getRuntime().runFinalization();
		Runtime.getRuntime().halt(1);				
	}
}
