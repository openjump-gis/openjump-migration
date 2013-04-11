package org.openjump.core.ui.plugin.datastore.postgis;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;

/**
 * static methods to help formatting sql statements for PostGIS
 */
public class PostGISConnectionUtil {
    
    Connection connection;
    
    public PostGISConnectionUtil(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Returns the geometry dimension defined in geometry_columns for this table.
     */
    public int getGeometryDimension(String dbSchema, String dbTable, int defaultDim) {
        try {
            StringBuilder query = new StringBuilder("SELECT coord_dimension FROM geometry_columns WHERE ");
            if (dbSchema != null) query.append("f_table_schema = '" + dbSchema + "' AND ");
            query.append("f_table_name = '" + dbTable + "';");
            ResultSet rs = connection.createStatement().executeQuery(query.toString());
            if (rs.next()) return rs.getInt(1);
            else return defaultDim;
        } catch(SQLException sqle) {
            return defaultDim;
        }
    }
    
    /**
     * Returns the srid defined in geometry_columns for this table.
     */
    public int getGeometrySrid(String dbSchema, String dbTable, int defaultSrid) {
        try {
            StringBuilder query = new StringBuilder("SELECT srid FROM geometry_columns WHERE ");
            if (dbSchema != null) query.append("f_table_schema = '" + dbSchema + "' AND ");
            query.append("f_table_name = '" + dbTable + "';");
            ResultSet rs = connection.createStatement().executeQuery(query.toString());
            if (rs.next()) return rs.getInt(1);
            else return defaultSrid;
        } catch(SQLException sqle) {
            return defaultSrid;
        }
    }
    
    /** 
     * Returns a list of attributes compatibe between postgis table and
     * featureSchema.
     */
    public String[] compatibleSchemaSubset(String dbSchema, String dbTable, 
                              FeatureSchema featureSchema) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        ResultSet rs = metadata.getColumns(null,
                PostGISQueryUtil.unquote(dbSchema), 
                PostGISQueryUtil.unquote(dbTable), null);
        Map<String,AttributeType> map = new HashMap<String,AttributeType>();
        while (rs.next()) {
            map.put(rs.getString("COLUMN_NAME"),
                PostGISQueryUtil.getAttributeType(rs.getInt("DATA_TYPE"), rs.getString("TYPE_NAME")));
        }
        List<String> subset = new ArrayList<String>();
        for (int i = 0 ; i < featureSchema.getAttributeCount() ; i++) {
            String attribute = featureSchema.getAttributeName(i);
            AttributeType type = featureSchema.getAttributeType(i);
            if (map.containsKey(attribute) && (map.get(attribute)==type)) {
                subset.add(attribute);
            }
        }
        return subset.toArray(new String[subset.size()]);
    }

}