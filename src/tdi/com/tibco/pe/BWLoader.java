package tdi.com.tibco.pe;

/*** COMPATIBILITY CLASS, OLD INTERFACE ***/
public class BWLoader implements Runnable {
	
	public void run() {
		try {
			PE.instrument(null);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public final static void main(String[] args) {
		String libpath = null;
		for (String s : System.getProperty("java.class.path").split(System.getProperty("path.separator"))) {
			if (s.contains("hotfix") && s.contains("bw")) {
				s = s.replace('/', '\\');
				if (s.endsWith("lib") || s.endsWith("lib\\")) {
					libpath = s + "\\tdi.jar";
					break;
				}
			}
		}
		System.out.println(libpath);
		javax.swing.JOptionPane.showMessageDialog(null, "Please restart Tibco Designer (end all processes an start again)", "TDI installed", javax.swing.JOptionPane.WARNING_MESSAGE);
	}
}
