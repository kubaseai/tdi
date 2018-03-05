package tdi.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.tibco.xml.channel.infoset.helpers.XmlContentTracer;

public class XmlContentTracerCompanion {
	
	public static void init() {
		System.setProperty("trace.raw.transformer", "true");
		try {
			Class<?> c = Class.forName("com.tibco.xml.xdata.bind.OnceOnlyBinding");
			for (Field f : c.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers()) && boolean.class.equals(f.getType())) {
					String name = f.getName().toLowerCase();
					if ((name.contains("transformer") && 
						(name.contains("trace") || name.contains("raw"))) ||
						name.equals("new")) {
						f.setAccessible(true);
						f.set(null, true);						
					}
				}
			}
		}
		catch (Exception e) {}
		new XmlContentTracer();
	}
	
	public static String getDomProfiling() {
		try {
			return XmlContentTracer.popResultForThread(Thread.currentThread().getId());
		}
		catch (Throwable t) {}
		return null;
	}
}
