package com.vividsolutions.jump.datastore.postgis;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.AdhocQuery;
import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesSQLBuilder;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.FeatureInputStream;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.ErrorDialog;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author nicolas
 */
public class PostgisDataStoreConnection extends SpatialDataStoreConnection {

    public PostgisDataStoreConnection(Connection con) {
        super(con); // ?
        connection = con;
        this.dbMetadata = new PostgisDataStoreMetadata(this);
    }
    
    @Override
    public SpatialDatabasesSQLBuilder getSqlBuilder(SpatialReferenceSystemID srid, String[] colNames) {
      return new PostgisSQLBuilder(this.dbMetadata, srid, colNames);
    }

    /**
     * Executes a filter query.
     *
     * The SRID is optional for queries - it will be determined automatically
     * from the table metadata if not supplied.
     * 13 dec 2015: query is now tested before execution, to prevent adding an empty
     * layer built from invalid WHERE clause, for instance.
     *
     * @param query the query to execute
     * @return the results of the query
     * @throws SQLException
     */
    @Override
    public FeatureInputStream executeFilterQuery(FilterQuery query) throws SQLException {
        SpatialReferenceSystemID srid = dbMetadata.getSRID(query.getDatasetName(), query.getGeometryAttributeName());
        String[] colNames = dbMetadata.getColumnNames(query.getDatasetName());

        PostgisSQLBuilder builder = (PostgisSQLBuilder)this.getSqlBuilder(srid, colNames);
        // [mmichaud 2013-08-07] add a parameter for database primary key name
        return new PostgisFeatureInputStream(connection, builder.getSQL(query).getQuery(), 
            query.getPrimaryKey());
    }
    
    /**
     * Executes an adhoc query.
     *
     * The SRID is optional for queries - it will be determined automatically
     * from the table metadata if not supplied.
     *
     * @param query the query to execute
     * @return the results of the query
     * @throws SQLException
     */
    @Override
    public FeatureInputStream executeAdhocQuery(AdhocQuery query) throws Exception {
        String queryString = query.getQuery();
        PostgisFeatureInputStream ifs = new PostgisFeatureInputStream(connection, queryString, query.getPrimaryKey());
        
        // Nicolas Ribot: getting FeatureSchema here actually runs the query: if an error occurs, must trap it here
        FeatureSchema fs = null;
        try {
          fs = ifs.getFeatureSchema();
        } catch (Exception e) {
          throw new Exception(
            I18N.get(SpatialDataStoreConnection.class.getName()                     
                +".SQL-error") + e.getMessage());
        }
        
        if (fs.getGeometryIndex() < 0) {
            throw new Exception(I18N.get(SpatialDataStoreConnection.class.getName()
                +".resultset-must-have-a-geometry-column"));
        }
        return ifs;
        
    }
    
}
