package tdi.com.tibco.pe.core;

public class MockedCallCustomProcessActivity extends com.tibco.pe.core.CallCustomProcessActivity {
	
	private static final long serialVersionUID = 1L;

	public void destroy() throws Exception {
		try {
			super.destroy();
		}
		catch (Throwable t) {}
	}
}
