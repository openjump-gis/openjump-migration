/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump.plugin.edit;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.noding.*;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jump.feature.Feature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class used for noded Geometry reconstruction. It has two public
 * methods :
 * <ul>
 * <li>One to build a Geometry from a set of noded SegmentString carrying their
 * {@link SegmentStringData}</li>
 * <li>The second to build a full map containing Features and their 
 * corresponding SegmentString's Map</li>
 * </ul>
 */
public class SegmentStringsWithData2Features {

    private SegmentStringsWithData2Features() {}
    
    /**
     * Build the new noded geometry from the source geometry and the structured
     * map of SegmentStrings.
     * @param source the source geometry
     * @param nodedSegmentStrings the hierarchical map of noded segment strings
     * @param interpolate_z whether the z of SegmentString ends must be 
     *        interpolated or not
     * @param interpolated_z_dp is the number of decimal digits to keep in
     *        interpolated z values
     */
    public static Geometry buildGeometry(Geometry source, 
            Map<Integer,Map<Integer,List<SegmentString>>> nodedSegmentStrings,
            boolean interpolate_z, int interpolated_z_dp) {
        // Number of components
        GeometryFactory gf = source.getFactory();
        Geometry[] geoms = new Geometry[nodedSegmentStrings.size()];
        for (int i = 0 ; i < geoms.length ; i++) {
            Geometry sourceComponent = source.getGeometryN(i);
            Map<Integer,List<SegmentString>> lines = nodedSegmentStrings.get(i);
            if (sourceComponent instanceof LineString) {
                geoms[i] = merge(lines.get(0), gf, false);
                if (interpolate_z) {
                    interpolate(lines.get(0), (LineString)geoms[i], interpolated_z_dp);
                }
            }
            else if (sourceComponent instanceof Polygon) {
                LinearRing exteriorRing = (LinearRing)merge(lines.get(0), gf, true);
                if (interpolate_z) {
                    interpolate(lines.get(0), exteriorRing, interpolated_z_dp);
                }
                LinearRing[] holes = new LinearRing[lines.size()-1];
                for (int j = 0 ; j < holes.length ; j++) {
                    holes[j] = (LinearRing)merge(lines.get(j+1), gf, true);
                    if (interpolate_z) {
                        interpolate(lines.get(0), holes[j], interpolated_z_dp);
                    }
                }
                geoms[i] = source.getFactory().createPolygon(exteriorRing, holes);
            }
        }
        return source.getFactory().buildGeometry(Arrays.asList(geoms));
    }
    
    /** 
     * Merge SegementStrings and return either a LineString or a LinearRing).
     */
    private static LineString merge(List<SegmentString> list,
                                    GeometryFactory gf, boolean close) {
        LineMerger lineMerger = new LineMerger();
        for (SegmentString ss : list) {
            lineMerger.add(gf.createLineString(ss.getCoordinates()));
        }
        LineString ls = (LineString)lineMerger.getMergedLineStrings().iterator().next();
        if (close) {
            CoordinateList coords = new CoordinateList(ls.getCoordinates());
            coords.closeRing();
            return gf.createLinearRing(coords.toCoordinateArray());
        }
        else {
            return ls;
        }
    }
    
    private static void interpolate(List<SegmentString> list, LineString g, int interpolated_z_dp) {
        for (SegmentString ss : list) {
            Coordinate[] cc = ss.getCoordinates();
            if (Double.isNaN(cc[0].z)) {
                cc[0].z = interpolate(cc[0], g, interpolated_z_dp);
            }
            if (Double.isNaN(cc[cc.length-1].z)) {
                cc[cc.length-1].z = interpolate(cc[cc.length-1], g, interpolated_z_dp);
            }
        }
    }
    
    private static double round(double d, int dp) {
        double scale = 1.0;
        for (int i = 0 ; i < dp ; i++) scale *= 10.0;
        return Math.rint(d*scale)/scale;
    } 
    
    /**
     * Interpolate the z of coordinate having indice c between coordinates
     * having prev and next indices in cc coordinate array.
     */
    private static double interpolate(Coordinate c, LineString line, int dp) {
        int prevIndex = -1;
        int index = -1;
        int nextIndex = -1;
        Coordinate[] cc = line.getCoordinates();
        for (int i = 0 ; i < cc.length ; i++) {
            if (index==-1 && c.equals(cc[i])) index = i;
            else if (Double.isNaN(cc[i].z)) continue;
            else if (index==-1) prevIndex = i;
            else {nextIndex = i; break;}
        }
        if (prevIndex > -1 && nextIndex > -1) {
            return round(interpolate(index, prevIndex, nextIndex, cc), dp);
        }
        else {
            return c.z;
        }
    }
    
    /**
     * Interpolate the z of coordinate having indice c between coordinates
     * having prev and next indices in cc coordinate array.
     */
    private static double interpolate(int c, int prev, int next, Coordinate[] cc) {
        double dBefor = 0.0;
        double dAfter = 0.0;
        for (int i = prev ; i < c ; i++) dBefor += cc[i].distance(cc[i+1]);
        for (int i = c ; i < next ; i++) dAfter += cc[i].distance(cc[i+1]);
        return cc[prev].z + (cc[next].z-cc[prev].z) * (dBefor/(dBefor+dAfter));
    }
    
    /**
     * Creates a hierarchy structure containing all the noded SegmentStrings
     * derived from Geometry components and linear elements.
     */
    public static Map<Feature,Map<Integer,Map<Integer,List<SegmentString>>>> 
                    getFeature2SegmentStringTreeMap(Collection nodedSubstring) {
        // Cr�ation de geomStructureMap, une table regroupant les SegmentStrings
        // r�sultant du traitement en fonction du feature, du component, 
        // (GeometryCollection) et de l'�l�ment lin�aire d'origine.
        // geomStructureMap     : 1 Feature --> N components
        // componentMap         : 1 component --> N linearElements
        // segmentString List   : 1 linearElement --> N SegmentStringWithRef
        Map<Feature,Map<Integer,Map<Integer,List<SegmentString>>>> geomStructureMap = 
            new HashMap<Feature,Map<Integer,Map<Integer,List<SegmentString>>>>();
        for (Object line : nodedSubstring) {
            SegmentString ss = (SegmentString)line;
            SegmentStringData metadata = (SegmentStringData)ss.getData();
            Feature feature = metadata.getFeature();
            int component = metadata.getComponent();
            int linearElement = metadata.getLinearElement();
            // R�cup�rer la table pour ce Feature
            Map<Integer,Map<Integer,List<SegmentString>>> components = geomStructureMap.get(feature);
            // Ce Feature n'a pas encore d'entr�e
            if (components == null) {
                components = new HashMap<Integer,Map<Integer,List<SegmentString>>>(1);
                Map<Integer,List<SegmentString>> linearElements = new HashMap<Integer,List<SegmentString>>(1);
                List<SegmentString> ssl = new ArrayList<SegmentString>(2);
                ssl.add(ss);
                linearElements.put(linearElement, ssl);
                components.put(component, linearElements);
                geomStructureMap.put(feature, components);
            }
            else {
                Map<Integer,List<SegmentString>> linearElements = components.get(component);
                if (linearElements == null) {
                    linearElements = new HashMap<Integer,List<SegmentString>>(1);
                    List<SegmentString> ssl = new ArrayList<SegmentString>(2);
                    ssl.add(ss);
                    linearElements.put(linearElement, ssl);
                    components.put(component, linearElements);
                }
                else {
                    List<SegmentString> ssl = linearElements.get(linearElement);
                    if (ssl == null) {
                        ssl = new ArrayList<SegmentString>(2);
                    }
                    ssl.add(ss);
                    linearElements.put(linearElement, ssl);
                }
            }
        }
        return geomStructureMap;
    }
        
}
