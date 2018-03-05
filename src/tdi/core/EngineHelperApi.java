package tdi.core;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.tibco.pe.core.JobContainer;
import com.tibco.pe.core.JobData;

/*** This is EngineHelper with external world dependencies ***/
public class EngineHelperApi extends com.tibco.pe.core.EngineHelper {
	
	private static XStream xs = new XStream(new DomDriver());
	
	public static void resumeHibernatedJobFromXml(String xml) throws Exception {
		JobExtractor je = (JobExtractor) xs.fromXML(xml);
		JobData jd = com.tibco.pe.core.EngineHelper.getImportedJob(je);
		com.tibco.pe.core.EngineHelper.resumeHibernatedJob(jd.jid, jd);
	}
	
	public static String hibernateJobToXml(long jid) throws Exception {
		String xml = null;
		JobContainer jc = com.tibco.pe.core.EngineHelper.hibernateJob(jid);
		JobData jd = new JobData("Job-"+jid, jc.job);
		xml = xs.toXML(com.tibco.pe.core.EngineHelper.getExtractedJobData(jd));
		return xml;
	}

}
