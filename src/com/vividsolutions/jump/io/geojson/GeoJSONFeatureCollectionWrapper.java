package com.vividsolutions.jump.io.geojson;

import java.io.IOException;
import java.io.Writer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.geojson.GeoJsonWriter;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;

/**
 * a wrapper for a feature collection to do funky geojson stuff to/with
 *
 */
public class GeoJSONFeatureCollectionWrapper implements JSONStreamAware {
  MapGeoJsonGeometryReader geomReader = null;
  FlexibleFeatureSchema featureSchema = null;
  FeatureCollection featureCollection = null;
  List<String> columnsWithMixedValues = new LinkedList<String>();

  /**
   * create a new empty FeatureCollection wrapper
   */
  public GeoJSONFeatureCollectionWrapper() {
    this.featureSchema = new FlexibleFeatureSchema();
    this.featureCollection = new FeatureDataset(featureSchema);
  }

  /**
   * create a wrapper for an existing FeatureCollection
   */
  public GeoJSONFeatureCollectionWrapper(FeatureCollection fc) {
    this.featureSchema = new FlexibleFeatureSchema(fc.getFeatureSchema());
    this.featureCollection = fc;
  }

  /**
   * add a Feature defined by given JSON-simple map the to the collection
   */
  public void add(Map featureMap) throws Exception {

    // this type of feature "autoextends" by returning null for undefined
    // attribs
    Feature feature = new BasicFeature(featureSchema) {

      @Override
      public Object getAttribute(int i) {
        if (i < 0)
          throw new InvalidParameterException(
              "index must be greater or equal zero");

        Object attrib = null;

        Object[] attributes = getAttributes();
        // only grab attrib if stack holds it already
        if (i < attributes.length)
          attrib = attributes[i];

        // OJ does not allow null geoms!
        if (i == featureSchema.getGeometryIndex()) {
          // create and set an empty geom
          if (attrib == null) {
            attrib = featureSchema.createEmptyGeometry();
            setGeometry((Geometry) attrib);
          }
        }

        return attrib;
      }

      /**
       * setting an attribute, fixing the underlying array in case the schema
       * changed inbetween
       */
      public void setAttribute(int attributeIndex, Object newAttribute) {
        FeatureSchema schema = getSchema();
        Object[] oldAttribs = getAttributes();
        // add fields if schema changed in between
        int diffCount = schema.getAttributeCount() - oldAttribs.length;
        if (diffCount > 0) {
          List attributes = new ArrayList(Arrays.asList(oldAttribs));
          attributes.addAll(Arrays.asList(new Object[diffCount]));
          super.setAttributes(attributes.toArray());
        }
        super.setAttribute(attributeIndex, newAttribute);
      }

      /**
       * setting the geometry by explicitly using the flexible setAttribute()
       * method above
       */
      public void setGeometry(Geometry geometry) {
        setAttribute(getSchema().getGeometryIndex(), geometry);
      }

      /**
       * getting the geometry by explicitly using the flexible getAttribute()
       * method above
       */
      public Geometry getGeometry() {
        return (Geometry) getAttribute(getSchema().getGeometryIndex());
      }
    };

    // parse geometry
    if (featureMap.containsKey(GeoJSONConstants.GEOMETRY)
        && (featureMap.get(GeoJSONConstants.GEOMETRY) instanceof Map)) {
      // add geom attribute if none so far
      if (featureSchema.getGeometryIndex() < 0) {
        featureSchema.addAttribute("Geometry", AttributeType.GEOMETRY);
      }

      Map geometryMap = (Map) featureMap.get(GeoJSONConstants.GEOMETRY);
      // initialize geom reader
      if (geomReader == null)
        geomReader = new MapGeoJsonGeometryReader();

      Geometry geom = geomReader.read(geometryMap);
      // memorize a geomtype from the dataset
      if (featureSchema.getGeometryType() == null)
        featureSchema.setGeometryType(geom.getClass());

      feature.setGeometry(geom);
    }

    // parse attributes
    Map<String, Object> attribsMap = null;
    if (featureMap.containsKey(GeoJSONConstants.PROPERTIES)
        && featureMap.get(GeoJSONConstants.PROPERTIES) instanceof Map) {
      attribsMap = (Map) featureMap.get(GeoJSONConstants.PROPERTIES);
      // iterate over this feature's attribs
      for (String key : attribsMap.keySet()) {
        Object value = attribsMap.get(key);
        AttributeType type = toAttributeType(value);

        // extend schema if attrib is unknown
        if (!featureSchema.hasAttribute(key)) {
          featureSchema.addAttribute(key, type);
        }
        // detect mixedType columns to fixup Schema later
        else if (!columnsWithMixedValues.contains(key)
            && featureSchema.getAttributeType(key) != type) {
          // this column had null until now
          if (featureSchema.getAttributeType(key) == ATTRIBUTETYPE_NULL) {
            featureSchema.setAttributeType(key, type);
          } 
          // this column hosts mixed attrib types
          else {
            columnsWithMixedValues.add(key);
          }
        }

        feature.setAttribute(key, value);
      }
    }

    featureCollection.add(feature);
  }

  static class Null extends Object{
  };

  static class NullAttributeType extends AttributeType {
    public NullAttributeType() {
      super("NULL", Null.class);
    }
  };

  public static final AttributeType ATTRIBUTETYPE_NULL = new NullAttributeType();

  public static AttributeType toAttributeType(Object value){
    // for null values we use temporarily a custom attrib type which get's fixed in getFeatCol()
    if ( value == null )
      return ATTRIBUTETYPE_NULL;
    AttributeType type = AttributeType.toAttributeType(value.getClass());
    // unknown mappings return null, we assume Object then
    if ( type == null )
      type = AttributeType.OBJECT;
    return type;
  }

  public int size() {
    return featureCollection.size();
  }

  /**
   * we need to fixup the feature schema before the collection is ready to be
   * used
   * 
   * @return
   */
  public FeatureCollection getFeatureCollection() {
    // set type to String for mixed columns
    for (String key : columnsWithMixedValues) {
      featureSchema.setAttributeType(featureSchema.getAttributeIndex(key),
          AttributeType.STRING);
      columnsWithMixedValues.remove(key);
    }
    // set type to String for the internal ATTRIBUTETYPE_NULL columns
    for (int i = 0; i < featureSchema.getAttributeCount(); i++) {
      AttributeType type = featureSchema.getAttributeType(i);
      if ( type == ATTRIBUTETYPE_NULL )
        featureSchema.setAttributeType(i, AttributeType.STRING);
    }
    return featureCollection;
  }

  @Override
  public void writeJSONString(Writer out) throws IOException {
    out.write("{\n");
    out.write("\"type\": \"" + GeoJSONConstants.TYPE_FEATURECOLLECTION
        + "\",\n\n");
    out.write("\"" + GeoJSONConstants.FEATURES + "\": [\n");

    boolean first = true;
    String[] featureFields = new String[] { GeoJSONConstants.TYPE,
        GeoJSONConstants.PROPERTIES, GeoJSONConstants.GEOMETRY };
    for (Feature feature : featureCollection.getFeatures()) {
      if (first)
        first = false;
      else
        out.write(",\n");

      String featureJson = toJSONString(feature);
      out.write(featureJson);
    }
    out.write("\n]");

    out.write("\n\n}");
  }

  /**
   * Escape quotes, \, /, \r, \n, \b, \f, \t and other control characters
   * (U+0000 through U+001F). It's the same as JSONValue.escape() only for
   * compatibility here.
   * 
   * @see org.json.simple.JSONValue#escape(String)
   * 
   * @param s
   * @return
   */
  public static String escape(String s) {
    return JSONValue.escape(s);
  }

  public static String toJSONString(Feature feature) {
    String propertiesJson = null, geometryJson = null;
    FeatureSchema schema = feature.getSchema();

    for (int i = 0; i < schema.getAttributeCount(); i++) {
      String name = schema.getAttributeName(i);
      AttributeType type = schema.getAttributeType(i);
      Object value = feature.getAttribute(i);

      if (i == schema.getGeometryIndex()) {
        Geometry geometry = (Geometry) value;
        if (geometry != null)
          geometryJson = new GeoJsonWriter().write(geometry);
      } else {
        String json = JSONObject.toString(name, value);
        propertiesJson = propertiesJson != null ? propertiesJson + ", " + json
            : json;
      }
    }

    if (geometryJson != null)
      geometryJson = "\"geometry\": " + geometryJson;
    if (propertiesJson != null)
      propertiesJson = "\"properties\": { " + propertiesJson + " }";

    return "{ \"type\": \"Feature\""
        + (propertiesJson != null ? ", " + propertiesJson : "")
        + (geometryJson != null ? ", " + geometryJson : "") + " }";
  }
}
