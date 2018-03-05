package tdi.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class JDBCDriver implements java.sql.Driver {
	
	private static JDBCDriver INSTANCE = null;
	private org.hsqldb.jdbc.JDBCDriver superDriver = new org.hsqldb.jdbc.JDBCDriver();
	
	static {
		try {
			INSTANCE = new JDBCDriver();
			DriverManager.registerDriver(INSTANCE);		
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}	
		
	public boolean acceptsURL(String url) {
		return url!=null && url.startsWith("tdi:");
	}
	
	public Connection connect(String url, Properties props) throws SQLException {
		if (url!=null && url.startsWith("tdi:mem:"))
			url = "jdbc:hsqldb:"+url.substring(4);
		else if (url!=null && url.startsWith("tdi:")) {
			url = url.substring(4);
			while (url.charAt(0) == '/')
				url = url.substring(1);
			url = "jdbc:hsqldb:hsql://"+url+"/tdi";
		}		
			
		if (props==null)
			props = new Properties();
		if (props.get("user")==null) {
			props.put("user", "sa");
			props.put("password", "");
		}
		props.put("url", url);
		props.put("hsqldb.lock_file", "false");
		props.put("sql.syntax_ora", "true");
		return superDriver.connect(url, props);
	}
	
	public static Connection getConnection(String url, Properties props) throws SQLException {
		return INSTANCE.connect(url, props);
	}
	
	public final static void main(String[] args) throws SQLException {
		new JDBCDriver();
		Connection conn = DriverManager.getConnection("tdi://localhost:8192");
		conn.close();
	}

	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
			throws SQLException {
		return superDriver.getPropertyInfo(url, info);
	}

	public int getMajorVersion() {
		return 1;
	}

	public int getMinorVersion() {		
		return 0;
	}

	public boolean jdbcCompliant() {
		return true;
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return superDriver.getParentLogger();
	}
}
