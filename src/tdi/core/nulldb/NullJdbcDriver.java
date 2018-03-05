package tdi.core.nulldb;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Logger;

public class NullJdbcDriver implements java.sql.Driver {
	
	static {
		Enumeration<Driver> en = DriverManager.getDrivers();
		while (en.hasMoreElements()) {
			Driver drv = en.nextElement();
			try {
				if (!drv.acceptsURL("tdi:"))
					DriverManager.deregisterDriver(drv);
			}
			catch (SQLException e) {}
		}
		try {
			DriverManager.registerDriver(new NullJdbcDriver());
		}
		catch (SQLException e) {}
	}

	public boolean acceptsURL(String url) throws SQLException {
		return url!=null && !url.startsWith("tdi:");
	}

	public Connection connect(String url, Properties info) throws SQLException {
		return new NullConnection(url, info);
	}

	public int getMajorVersion() {
		return 1;
	}

	public int getMinorVersion() {
		return 0;
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}

	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		DriverPropertyInfo dpi = new DriverPropertyInfo("NullJdbcDriver", "Returns dummy connections");
		return new DriverPropertyInfo[] { dpi };
	}

	public boolean jdbcCompliant() {
		return true;
	}
	
	public final static void main(String[] args) throws SQLException {
		PreparedStatement ps = new NullJdbcDriver().connect("localhost", null).prepareStatement("select 1 from dual");
		ResultSet rs = ps.executeQuery();
		System.out.println(rs.getString(1));
		
	}

}
