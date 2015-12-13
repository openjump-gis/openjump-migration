package com.vividsolutions.jump.datastore.oracle;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jump.datastore.jdbc.ValueConverter;
import com.vividsolutions.jump.datastore.jdbc.ValueConverterFactory;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesValueConverterFactory;
import com.vividsolutions.jump.feature.AttributeType;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 *
 */
public class OracleValueConverterFactory extends SpatialDatabasesValueConverterFactory {

  protected final ValueConverter ORA_STRUCT_GEOMETRY_MAPPER = new OracleStructGeometryValueConverter();

  class OracleStructGeometryValueConverter implements ValueConverter {

    public AttributeType getType() {
      return AttributeType.GEOMETRY;
    }

    public Object getValue(ResultSet rs, int columnIndex) throws IOException,
        SQLException, ParseException, ClassNotFoundException,
        NoSuchMethodException, SecurityException, InstantiationException,
        IllegalAccessException, IllegalArgumentException,
        InvocationTargetException {
      Object geometryObject = rs.getObject(columnIndex);
      Class converterClazz = Class
          .forName("org.geotools.data.oracle.sdo.GeometryConverter");
      Class connectionClazz = Class.forName("oracle.jdbc.OracleConnection");
      Class structClazz = Class.forName("oracle.sql.STRUCT");
      Method converterMethod = converterClazz.getMethod("asGeometry",
          new Class[]{structClazz});

      Constructor constructor = converterClazz
          .getDeclaredConstructor(connectionClazz);
      Object converter = constructor.newInstance(connectionClazz.cast(rs
          .getStatement().getConnection()));

      return converterMethod.invoke(converter, structClazz.cast(geometryObject));
    }
  }

  public OracleValueConverterFactory(Connection conn) {
    super(conn);
  }

  @Override
  public ValueConverter getConverter(ResultSetMetaData rsm, int columnIndex)
      throws SQLException {
    String dbTypeName = rsm.getColumnTypeName(columnIndex);

    // manages 2 cases: type retrieved from Database metadata (DataStore Panel)
    // and from direct Adhoc query (type of the column resultset).
    if (dbTypeName.equalsIgnoreCase("MDSYS.SDO_GEOMETRY")) {
      return ORA_STRUCT_GEOMETRY_MAPPER;
    }

    // handle the standard types
    ValueConverter stdConverter = ValueConverterFactory.getConverter(rsm, columnIndex);
    if (stdConverter != null) {
      return stdConverter;
    }

    // default - can always show it as a string!
    return ValueConverterFactory.STRING_MAPPER;
  }

}
