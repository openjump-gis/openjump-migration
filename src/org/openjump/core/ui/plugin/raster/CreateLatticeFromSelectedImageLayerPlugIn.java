/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This class implements extensions to JUMP and is
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
 * For more information, contact:
 * Stefan Steiniger
 * perriger@gmx.de
 */

/*****************************************************
 * created:  		27.Oct.2009
 * last modified:   					
 * 					
 * 
 * @author sstein
 * 
 * description:
 * 	
 *  
 *****************************************************/

package org.openjump.core.ui.plugin.raster;

import java.awt.geom.Point2D;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * @description: creates a lattice for the current selected raster image
 * @TODO: I was going todo this as a normal plugin, but this won't work since
 * raster images are Layerables and not layer objects, so the drop down list doesn't
 * display them
 *	
 * @author sstein
 *
 **/
public class CreateLatticeFromSelectedImageLayerPlugIn extends AbstractPlugIn implements ThreadedPlugIn{
  
    private PlugInContext context = null;
    
    GeometryFactory gfactory = new GeometryFactory();
        
    public void initialize(PlugInContext context) throws Exception {
    				
	        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
	    	featureInstaller.addMainMenuItem(
	    	        this,								//exe
	                new String[] {MenuNames.RASTER}, 	//menu path
	                "Create Lattice from Image", 
	                false,			//checkbox
	                null,			//icon
	                createEnableCheck(context.getWorkbenchContext())); //enable check
    }

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createAtLeastNLayerablesMustBeSelectedCheck(1, RasterImageLayer.class));
    }
    
    /**
     *@inheritDoc
     */
    public String getIconString() {
        return null;
    }
   
    
	public boolean execute(PlugInContext context) throws Exception{
		//-- not used here
        return true;	    
	}

	public void run(TaskMonitor monitor, PlugInContext context)
			throws Exception {
		monitor.allowCancellationRequests();
		GeometryFactory gf = new GeometryFactory();
		//-- get the rasterimage layer
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(context, RasterImageLayer.class);
        //System.out.println(rLayer);
        
        if (rLayer==null){
            context.getWorkbenchFrame().warnUser("no layer selected");
            return;
        }
		
		//-- create a sextante raster layer since it is easier to handle
		OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
		rstLayer.create(rLayer);
		// create a gridwrapper to later access the cells
		GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(rstLayer, rstLayer.getLayerGridExtent());
		//-- create the FeatureSchema
		FeatureSchema fs = new FeatureSchema();
		fs.addAttribute("geometry", AttributeType.GEOMETRY);
		fs.addAttribute("cellid_x", AttributeType.INTEGER);
		fs.addAttribute("cellid_y", AttributeType.INTEGER);
		int numBands = rstLayer.getBandsCount();
		for (int i = 0; i < numBands; i++) {
			fs.addAttribute("band_" + i, AttributeType.DOUBLE);
		}
		//-- create a new empty dataset
		FeatureCollection fd = new FeatureDataset(fs);
		//-- create points
		monitor.report("creating points");
		int nx = rstLayer.getLayerGridExtent().getNX();
		int ny = rstLayer.getLayerGridExtent().getNY();
		//int numPoints = nx * ny;
		for (int x = 0; x < nx; x++) {//cols
			for (int y = 0; y < ny; y++) {//rows
				Feature ftemp = new BasicFeature(fs); 
				Point2D pt = rstLayer.getLayerGridExtent().getWorldCoordsFromGridCoords(x, y);				
				Geometry centerPoint = gf.createPoint(new Coordinate(pt.getX(), pt.getY())); 
				ftemp.setGeometry(centerPoint);
				for (int i = 0; i < numBands; i++) {
					double value = gwrapper.getCellValueAsDouble(x, y, i);
					ftemp.setAttribute("band_" + i, value);
				}
				ftemp.setAttribute("cellid_x", x);
				ftemp.setAttribute("cellid_y", y);
				//-- add the feature
				fd.add(ftemp);
				//-- check if user wants to stop
				if(monitor.isCancelRequested()){
					if(fd.size() > 0){
						context.addLayer(StandardCategoryNames.RESULT, rstLayer.getName() + "_cancel_lattice", fd);
					}
					return;
				}
			}
		}
		//-- output
		if(fd.size() > 0){
			context.addLayer(StandardCategoryNames.RESULT, rstLayer.getName() + "_lattice", fd);
		}
	}
    
  
}
