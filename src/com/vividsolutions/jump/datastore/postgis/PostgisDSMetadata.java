package com.vividsolutions.jump.datastore.postgis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.datastore.jdbc.JDBCUtil;
import com.vividsolutions.jump.datastore.jdbc.ResultSetBlock;


public class PostgisDSMetadata implements DataStoreMetadata {

  private final WKBReader reader = new WKBReader();

  private PostgisDSConnection conn;

  private Map sridMap = new HashMap();

  public PostgisDSMetadata( PostgisDSConnection conn ) {
    this.conn = conn;
  }

  public String[] getDatasetNames() {
    final List datasetNames = new ArrayList();
    // Spatial tables only.
    JDBCUtil.execute(
        conn.getConnection(),
        "SELECT DISTINCT f_table_schema, f_table_name FROM geometry_columns",
        new ResultSetBlock() {
      public void yield( ResultSet resultSet ) throws SQLException {
        while ( resultSet.next() ) {
          String schema = resultSet.getString( 1 );
          String table = resultSet.getString( 2 );
          if ( !schema.equalsIgnoreCase( "public" ) ) {
            table = schema + "." + table;
          }
          datasetNames.add( table );
        }
      }
    } );
    return ( String[] ) datasetNames.toArray( new String[]{} );
  }


  public Envelope getExtents( String datasetName, String attributeName ) {
    final Envelope[] e = new Envelope[]{null};
    //
    // Use find_extent - sometimes estimated_extent was returning null
    // [mmichaud 2011-10-30] try ST_Estimated_Extent again, hope find_extent bug
    // is solved. On big tables, find extents is much sloooower
    //    
    String sql = "";
    String sql2 = "";
    if(datasetName.indexOf('.') != -1) {
        String[] parts = datasetName.split("\\.", 2);
        sql = "SELECT ST_AsBinary(ST_Estimated_Extent( '" + parts[0] + "', '" + parts[1] +"', '" + attributeName + "' ))";
        //sql2 = "SELECT ST_AsBinary(ST_Extent(" + attributeName + ")) FROM " + datasetName;
    } else {
        sql = "SELECT ST_AsBinary(ST_Estimated_Extent( '" + datasetName + "', '" + attributeName + "' ))";
        //sql2 = "SELECT ST_AsBinary(ST_Extent( '" + datasetName + "', '" + attributeName + "' ))";
    }
    sql2 = "SELECT ST_AsBinary(ST_Envelope(ST_Extent(\"" + attributeName + "\"))) FROM \"" + datasetName + "\"";
    final ResultSetBlock resultSetBlock = new ResultSetBlock() {
        public void yield( ResultSet resultSet ) throws Exception {
            if ( resultSet.next() ) {
                byte[] bytes = ( byte[] ) resultSet.getObject( 1 );
                if ( bytes != null ) {
                    Geometry geom = reader.read( bytes );
                    if ( geom != null ) {
                        e[0] = geom.getEnvelopeInternal();
                    }
                }
            }
        }
    };
    try {
        JDBCUtil.execute(conn.getConnection(), sql, resultSetBlock
        );
    } catch (Exception ex) {
        JDBCUtil.execute(conn.getConnection(), sql2, resultSetBlock);
    }
    return e[0];
  }

  @Deprecated
  public SpatialReferenceSystemID getSRID(String tableName, String colName)
          throws SQLException {
      String key = tableName + "#" + colName;
      if (!sridMap.containsKey(key)) {
          // not in cache, so query it
          String srid = querySRID(tableName, colName);
          sridMap.put(key, new SpatialReferenceSystemID(srid));
      }
      SpatialReferenceSystemID srid = (SpatialReferenceSystemID) sridMap
              .get(key);
      return srid;
  }

  @Deprecated
  private String querySRID(String tableName, String colName)
  {
    final StringBuffer srid = new StringBuffer();
    // Changed by Michael Michaud 2010-05-26 (throwed exception for empty tableName)
    // String sql = "SELECT getsrid(" + colName + ") FROM " + tableName + " LIMIT 1";
    String[] tokens = tableName.split("\\.", 2);
    String schema = tokens.length==2?tokens[0]:"public";
    String table = tokens.length==2?tokens[1]:tableName;
    String sql = "SELECT srid FROM geometry_columns where (f_table_schema = '" + schema + "' and f_table_name = '" + table + "')";
    // End of the fix
    JDBCUtil.execute(conn.getConnection(), sql, new ResultSetBlock() {
      public void yield(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
          srid.append(resultSet.getString(1));
        }
      }
    });

    return srid.toString();
  }

  @Deprecated
  public String[] getGeometryAttributeNames( String datasetName ) {
    final List geometryAttributeNames = new ArrayList();
    String sql = "SELECT f_geometry_column FROM geometry_columns "
               + geomColumnMetadataWhereClause( "f_table_schema", "f_table_name", datasetName );
    JDBCUtil.execute(
        conn.getConnection(), sql,
        new ResultSetBlock() {
      public void yield( ResultSet resultSet ) throws SQLException {
        while ( resultSet.next() ) {
          geometryAttributeNames.add( resultSet.getString( 1 ) );
        }
      }
    } );
    return ( String[] ) geometryAttributeNames.toArray( new String[]{} );
  }
  
  public List<GeometryColumn> getGeometryAttributes( String datasetName ) {
    final List<GeometryColumn> geometryAttributes = new ArrayList<GeometryColumn>();
    String sql = "SELECT f_geometry_column, srid, type FROM geometry_columns "
               + geomColumnMetadataWhereClause( "f_table_schema", "f_table_name", datasetName );
    JDBCUtil.execute(
        conn.getConnection(), sql,
        new ResultSetBlock() {
      public void yield( ResultSet resultSet ) throws SQLException {
        while ( resultSet.next() ) {
          geometryAttributes.add(new GeometryColumn(
              resultSet.getString(1),
              resultSet.getInt(2),
              resultSet.getString(3)));
        }
      }
    } );
    return geometryAttributes;
  }


  public String[] getColumnNames( String datasetName ) {
    String sql = "SELECT column_name FROM information_schema.columns "
               + geomColumnMetadataWhereClause( "table_schema", "table_name", datasetName );
    ColumnNameBlock block = new ColumnNameBlock();
    JDBCUtil.execute( conn.getConnection(), sql, block );
    return block.colName;
  }


  private String geomColumnMetadataWhereClause( String schemaCol, String tableCol, String tableName ) {
    // [mmichaud 2011-07-24] Fixed a bug related to tables having common
    // names in public schema and another schema
    int dotPos = tableName.indexOf( "." );
    String schema = "public";
    String table = tableName;
    if (dotPos != -1) {
        schema = tableName.substring( 0, dotPos );
        table = tableName.substring( dotPos + 1 );
    }
    return "WHERE " + schemaCol + " = '" + schema + "'"
          + " AND " + tableCol + " = '" + table + "'";
  }

  private static class ColumnNameBlock implements ResultSetBlock {
    List colList = new ArrayList();
    String[] colName;

    public void yield( ResultSet resultSet ) throws SQLException {
      while ( resultSet.next() ) {
        colList.add( resultSet.getString( 1 ) );
      }
      colName = ( String[] ) colList.toArray( new String[0] );
    }
  }
  
}