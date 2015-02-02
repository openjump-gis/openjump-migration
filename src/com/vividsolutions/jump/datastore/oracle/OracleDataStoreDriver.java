package com.vividsolutions.jump.datastore.oracle;

import java.sql.*;

import com.vividsolutions.jump.datastore.*;

import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.parameter.ParameterListSchema;

/**
 *  * A driver for supplying {@link OracleDSConnection}s
 */
public class OracleDataStoreDriver
    implements DataStoreDriver
{
  public static final String DRIVER_NAME = "Oracle Spatial";
  public static final String JDBC_CLASS = "oracle.jdbc.driver.OracleDriver";
  // using new URL style: jdbc:oracle:thin:@//[HOST][:PORT]/SERVICE
  public static final String URL_PREFIX = "jdbc:oracle:thin:@//";

  public static final String PARAM_Server = "Server";
  public static final String PARAM_Port = "Port";
  public static final String PARAM_Instance = "Database";
  public static final String PARAM_User = "User";
  public static final String PARAM_Password = "Password";

  private static final String[] paramNames = new String[] {
    PARAM_Server,
    PARAM_Port,
    PARAM_Instance,
    PARAM_User,
    PARAM_Password
    };
  private static final Class[] paramClasses = new Class[]
  {
    String.class,
    Integer.class,
    String.class,
    String.class,
    String.class
    };
  private final ParameterListSchema schema = new ParameterListSchema(paramNames, paramClasses);

  public OracleDataStoreDriver() {
  }

  public String getName()
  {
    return DRIVER_NAME;
  }
  public ParameterListSchema getParameterListSchema()
  {
    return schema;
  }
  public DataStoreConnection createConnection(ParameterList params)
      throws Exception
  {
    String host = params.getParameterString(PARAM_Server);
    int port = params.getParameterInt(PARAM_Port);
    String database = params.getParameterString(PARAM_Instance);
    String user = params.getParameterString(PARAM_User);
    String password = params.getParameterString(PARAM_Password);

    String url
        = String.valueOf(new StringBuffer(URL_PREFIX).append
        (host).append
        (":").append
        (port).append
        ("/").append(database));

    Driver driver = (Driver) Class.forName(JDBC_CLASS).newInstance();
    DriverManager.registerDriver(driver);

    // mmichaud 2013-08-27 workaround for ticket #330
    String savePreferIPv4Stack = System.getProperty("java.net.preferIPv4Stack");
    String savePreferIPv6Addresses = System.getProperty("java.net.preferIPv6Addresses");
    System.setProperty("java.net.preferIPv4Stack", "true");
    System.setProperty("java.net.preferIPv6Addresses", "false");

    Connection conn = DriverManager.getConnection(url, user, password);

    if (savePreferIPv4Stack == null) {
        System.getProperties().remove("java.net.preferIPv4Stack");
    } else {
        System.setProperty("java.net.preferIPv4Stack", savePreferIPv4Stack);
    }
    if (savePreferIPv6Addresses == null) {
        System.getProperties().remove("java.net.preferIPv6Addresses");
    } else {
        System.setProperty("java.net.preferIPv6Addresses", savePreferIPv6Addresses);
    }
    return new OracleDSConnection(conn);
  }
  public boolean isAdHocQuerySupported() {
      return true;
  }

}