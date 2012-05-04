/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
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
 */
 
package org.openjump.core.ui.plugin.tools;

//import java.lang.Object;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.openjump.core.ui.images.IconLoader;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;
//
//import com.vividsolutions.jts.geom.Coordinate;
//import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
//import com.vividsolutions.jts.geom.GeometryCollection;
//import com.vividsolutions.jts.geom.GeometryFactory;
//import com.vividsolutions.jts.geom.LineString;
//import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
//import com.vividsolutions.jts.triangulate.VertexTaggedGeometryDataMapper;
//import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;
//
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
//import com.vividsolutions.jump.feature.FeatureDatasetFactory;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
//
//import javax.swing.JCheckBox;


/**
 * User can add one or several of the following attribute to a layer.
 * <ul>
 * <li>X,Y</li>
 * <li>Z</li>
 * @author Micha&euml;l Michaud
 */

import com.vividsolutions.jump.feature.FeatureSchema;

public class AddGeometryAttributesPlugIn extends AbstractThreadedUiPlugIn{

    //public static String VORONOI_DIAGRAM     = I18N.get("org.openjump.core.ui.plugin.tools.VoronoiDiagramPlugIn.voronoi-diagram");
    public static String LAYER               = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.layer");
    
    public static String X                   = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.x");
    public static String Y                   = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.y");
    public static String ADD_XY              = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-xy");
    
    public static String Z                   = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.z");
    public static String ADD_Z               = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-z");
    
    public static String NB_POINTS           = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.nb-points");
    public static String ADD_NB_POINTS       = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-nb-of-points");
    
    public static String NB_HOLES            = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.nb-of-holes");
    public static String ADD_NB_HOLES        = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-nb-of-holes");
    
    public static String NB_COMPONENTS       = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.nb-of-components");
    public static String ADD_NB_COMPONENTS   = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-nb-of-components");
    
    public static String LENGTH              = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.length");
    public static String ADD_LENGTH          = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-length");
    
    public static String AREA                = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.area");
    public static String ADD_AREA            = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-area");
    
    public static String GEOM_TYPE           = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.geo-type");
    public static String ADD_GEOMETRY_TYPE   = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-geom-type");
    
    public static String WKT                 = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.wkt");
    public static String ADD_WKT             = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-wkt");

    public static String GEOM_ATTRIBUTES     = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.geometry-attributes");
    
    public static String COMPUTE_ATTRIBUTES  = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.compute-attributes"); 
    
    // Z_GEOMETRY 
    // public static String ADD_Z_INI           = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-initial-z");
    // public static String ADD_Z_FIN           = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-final-z");
    // public static String ADD_Z_MIN           = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-min-z");
    // public static String ADD_Z_MAX           = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-max-z");
    // public static String ADD_Z_MEAN          = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-mean-z");
    // 
    // // MORPHOLOGY
    // public static String ADD_ORIENTATION     = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-orientation");
    // // http://iahs.info/hsj/470/hysj_47_06_0921.pdf
    // // http://www.ipublishing.co.in/jggsvol1no12010/voltwo/EIJGGS3022.pdf
    // public static String ADD_GRAVELIUS          = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-miller");
    // public static String ADD_MILLER          = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-miller");
    
    public static String ADD_SINUOSITY       = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-miller");
    
    
          

    String layer;
    
    boolean addXY = true;
    boolean addZ  = false;
    boolean addNbPoints  = false;
    boolean addNbHoles  = false;
    boolean addNbComponents  = false;
    boolean addLength  = false;
    boolean addArea  = false;
    boolean addGeometryType  = false;
    boolean addWKT  = false;
    
    public void initialize(PlugInContext context) throws Exception {
    	    
	        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
	    	featureInstaller.addMainMenuItem(
	    	        this,
	                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES},
	                getName() + "...",
	                false,			//checkbox
	                null,           // Icon
	                createEnableCheck(context.getWorkbenchContext()));
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }
    
    
	public boolean execute(PlugInContext context) throws Exception{
	    this.reportNothingToUndoYet(context);
	        
 		MultiInputDialog dialog = new MultiInputDialog(
	            context.getWorkbenchFrame(), getName(), true);
	        setDialogValues(dialog, context);
	        GUIUtil.centreOnWindow(dialog);
	        dialog.setVisible(true);
	        if (! dialog.wasOKPressed()) { return false; }
	        getDialogValues(dialog);	    
	    return true;
	}
	
	public void setLayer(String sitesLayer) {
	    this.layer = layer;
	}
	
	public void setAddXY(boolean addXY) {
	    this.addXY = addXY;
	}
	
	public void setAddZ(boolean addZ) {
	    this.addZ = addZ;
	}
	
	public void setAddNbPoints(boolean addNbPoints) {
	    this.addNbPoints = addNbPoints;
	}
	
	public void setAddNbHoles(boolean addNbHoles) {
	    this.addNbHoles = addNbHoles;
	}
	
	public void setAddNbComponents(boolean addNbComponents) {
	    this.addNbComponents = addNbComponents;
	}
	
	public void setAddLength(boolean addLength) {
	    this.addLength = addLength;
	}
	
	public void setAddArea(boolean addArea) {
	    this.addArea = addArea;
	}
	
	public void setAddGeometryType(boolean addGeometryType) {
	    this.addGeometryType = addGeometryType;
	}
	
	public void setAddWKT(boolean addWKT) {
	    this.addWKT = addWKT;
	}
	
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
	    if (layer == null || context.getLayerManager().getLayer(layer) == null) {
	        layer = context.getCandidateLayer(0).getName();
	    }
    	dialog.addLayerComboBox(LAYER, context.getLayerManager().getLayer(layer), null, context.getLayerManager());
    	dialog.addCheckBox(ADD_XY, addXY);
    	dialog.addCheckBox(ADD_Z, addZ);
    	dialog.addCheckBox(ADD_NB_POINTS, addNbPoints);
    	dialog.addCheckBox(ADD_NB_HOLES, addNbHoles);
    	dialog.addCheckBox(ADD_NB_COMPONENTS, addNbComponents);
    	dialog.addCheckBox(ADD_LENGTH, addLength);
    	dialog.addCheckBox(ADD_AREA, addArea);
    	dialog.addCheckBox(ADD_GEOMETRY_TYPE, addGeometryType);
    	dialog.addCheckBox(ADD_WKT, addWKT);
    }

	private void getDialogValues(MultiInputDialog dialog) {
	    layer           = dialog.getLayer(LAYER).getName();
	    addXY           = dialog.getBoolean(ADD_XY);
	    addZ            = dialog.getBoolean(ADD_Z);
	    addNbPoints     = dialog.getBoolean(ADD_NB_POINTS);
	    addNbHoles      = dialog.getBoolean(ADD_NB_HOLES);
	    addNbComponents = dialog.getBoolean(ADD_NB_COMPONENTS);
	    addLength       = dialog.getBoolean(ADD_LENGTH);
	    addArea         = dialog.getBoolean(ADD_AREA);
	    addGeometryType = dialog.getBoolean(ADD_GEOMETRY_TYPE);
	    addWKT          = dialog.getBoolean(ADD_WKT);
    }
    
	
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.report(COMPUTE_ATTRIBUTES + "...");
        LayerManager layerManager = context.getLayerManager();
        FeatureCollection inputFC = layerManager.getLayer(layer).getFeatureCollectionWrapper();
        FeatureCollection result = new FeatureDataset(getNewSchema(layerManager.getLayer(layer)));
        for (Object o : inputFC.getFeatures()){
            Feature f = (Feature)o;
            Feature bf = new BasicFeature(result.getFeatureSchema());
            Object[] attributes = new Object[result.getFeatureSchema().getAttributeCount()];
            System.arraycopy(f.getAttributes(), 0, attributes, 0, f.getSchema().getAttributeCount());
            bf.setAttributes(attributes);
            setGeometryAttributes(bf);
            result.add(bf);
        }
        context.getLayerManager().addLayer(StandardCategoryNames.RESULT, layer+"-"+GEOM_ATTRIBUTES, result); 
    }
    
    private FeatureSchema getNewSchema(Layer layer) {
        FeatureSchema schema = (FeatureSchema)layer.getFeatureCollectionWrapper().getFeatureSchema().clone();
        if (addXY) {
            schema.addAttribute(X, AttributeType.DOUBLE);
            schema.addAttribute(Y, AttributeType.DOUBLE);
        }
        if (addZ)            schema.addAttribute(Z, AttributeType.DOUBLE);
        if (addNbPoints)     schema.addAttribute(NB_POINTS, AttributeType.INTEGER);
        if (addNbHoles)      schema.addAttribute(NB_HOLES, AttributeType.INTEGER);
        if (addNbComponents) schema.addAttribute(NB_COMPONENTS, AttributeType.INTEGER);
        if (addLength)       schema.addAttribute(LENGTH, AttributeType.DOUBLE);
        if (addArea)         schema.addAttribute(AREA, AttributeType.DOUBLE);
        if (addGeometryType) schema.addAttribute(GEOM_TYPE, AttributeType.STRING);
        if (addWKT)          schema.addAttribute(WKT, AttributeType.STRING);
        return schema;
    }
    
    private void setGeometryAttributes(Feature f) {
        Geometry g = f.getGeometry();
        if (addXY) {
            f.setAttribute(X, g.getCoordinate().x);
            f.setAttribute(Y, g.getCoordinate().y);
        }
        if (addZ) f.setAttribute(Z, g.getCoordinate().z);
        if (addNbPoints) f.setAttribute(NB_POINTS, g.getCoordinates().length);
        if (addNbHoles) {
            int h = 0;
            for (int i = 0 ; i < g.getNumGeometries() ; i++) {
                Geometry component = g.getGeometryN(i);
                if (component instanceof Polygon) {
                    h += ((Polygon)component).getNumInteriorRing();
                }
            }
            f.setAttribute(NB_HOLES, h);
        }
        if (addNbComponents) f.setAttribute(NB_COMPONENTS, g.getNumGeometries());
        if (addLength) f.setAttribute(LENGTH, g.getLength());
        if (addArea) f.setAttribute(AREA, g.getArea());
        if (addGeometryType) f.setAttribute(GEOM_TYPE, g.getGeometryType());
        if (addWKT) f.setAttribute(WKT, g.toString());
    }
    
	
}
