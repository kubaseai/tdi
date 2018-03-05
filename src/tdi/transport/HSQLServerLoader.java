package tdi.transport;

import java.io.File;
import java.io.PrintStream;
import tdi.core.ProtectingClassLoader;

public class HSQLServerLoader {
	
	public final static void main(String[] args) {
		try {
			PrintStream outErr = new PrintStream(new File("./db.log"));
			System.setOut(outErr);
			System.setErr(outErr);
		}
		catch (Exception exc) {}
		ProtectingClassLoader loader = null;
		try {    
        	loader = new ProtectingClassLoader();
        	loader.loadClass("tdi.transport.HSQLServer")
        	.getDeclaredMethod("main", String[].class).invoke(null, (Object)args);
        	loader.close();
		}
        catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
