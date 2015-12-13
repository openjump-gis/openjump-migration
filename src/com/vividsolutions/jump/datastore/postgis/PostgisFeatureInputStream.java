package com.vividsolutions.jump.datastore.postgis;

import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesFeatureInputStream;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesResultSetConverter;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nicolas Ribot
 */
public class PostgisFeatureInputStream extends SpatialDatabasesFeatureInputStream {

  public PostgisFeatureInputStream(Connection conn, String queryString) {
        this(conn, queryString, null);
    }

    public PostgisFeatureInputStream(Connection conn, String queryString, String externalIdentifier) {
        super(conn, queryString, externalIdentifier);
    try {
      JUMPWorkbench.getInstance().getFrame().log("creating a PostgisFeatureInputStream (class:" + this.getClass()
          + " ) (driver: " + conn.getMetaData().getDriverName() + ") id"
          + this.hashCode(), this.getClass());
    } catch (SQLException ex) {
      Logger.getLogger(PostgisFeatureInputStream.class.getName()).log(Level.SEVERE, null, ex);
    }
    }
    
    /**
     * Returns a PostgisResultSetConverter
     * @param rs
     * @return 
     */
    @Override
    protected SpatialDatabasesResultSetConverter getResultSetConverter(ResultSet rs) {
      return new PostgisResultSetConverter(conn, rs);
    }
}
