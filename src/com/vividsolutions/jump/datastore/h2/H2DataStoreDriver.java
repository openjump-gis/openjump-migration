package com.vividsolutions.jump.datastore.h2;

import java.sql.Connection;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDataStoreDriver;
import com.vividsolutions.jump.parameter.ParameterList;

/**
 * A driver for supplying
 * {@link com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection}
 * s
 */
public class H2DataStoreDriver extends AbstractSpatialDatabasesDataStoreDriver {

  public final static String JDBC_CLASS = "org.h2.Driver";

  public H2DataStoreDriver() {
    this.driverName = "H2GIS";
    this.jdbcClass = "org.h2.Driver";
    this.urlPrefix = "jdbc:h2:";
  }

  /**
   * returns the right type of DataStoreConnection
   * 
   * @param params
   * @return
   * @throws Exception
   */
  @Override
  public DataStoreConnection createConnection(ParameterList params)
      throws Exception {
    Connection conn = super.createJdbcConnection(params);
    return new H2DSConnection(conn);
  }
}
