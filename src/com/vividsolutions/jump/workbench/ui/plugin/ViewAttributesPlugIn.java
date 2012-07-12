/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for
 * visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * For more information, contact:
 * 
 * Vivid Solutions Suite #1A 2328 Government Street Victoria BC V8T 5G5 Canada
 * 
 * (250)385-6040 www.vividsolutions.com
 */
package com.vividsolutions.jump.workbench.ui.plugin;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import java.awt.BorderLayout;
import javax.swing.ImageIcon;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import org.openjump.core.ui.swing.DetachableInternalFrame;
public class ViewAttributesPlugIn extends AbstractPlugIn {
	
	// Blackboard keys
	public static final String BB_ATTRIBUTES_WINDOW_SIZE_WIDTH = ViewAttributesPlugIn.class.getName() + " - ATTRIBUTES_WINDOW_SIZE_WIDTH";
	public static final String BB_ATTRIBUTES_WINDOW_SIZE_HEIGHT = ViewAttributesPlugIn.class.getName() + " - ATTRIBUTES_WINDOW_SIZE_HEIGHT";
	public static final String BB_ATTRIBUTES_WINDOW_POSITION_X = ViewAttributesPlugIn.class.getName() + " - ATTRIBUTES_WINDOW_POSITION_X";
	public static final String BB_ATTRIBUTES_WINDOW_POSITION_Y = ViewAttributesPlugIn.class.getName() + " - ATTRIBUTES_WINDOW_POSITION_Y";
	
	private static Blackboard blackboard = null;
	
	public ViewAttributesPlugIn() {
	}

	@Override
	public void initialize(PlugInContext context) throws Exception {
		super.initialize(context);
		blackboard = PersistentBlackboardPlugIn.get(context.getWorkbenchContext());
	}
	
	@Override
	public String getName() {
		return I18N.get("ui.plugin.ViewAttributesPlugIn.view-edit-attributes");
	}
	@Override
	public boolean execute(final PlugInContext context) throws Exception {
		reportNothingToUndoYet(context);
		//Don't add GeometryInfoFrame because the HTML will probably be too
		// much for the editor pane (too many features). [Jon Aquino]
		final ViewAttributesFrame frame = new ViewAttributesFrame(context
				.getSelectedLayer(0), context);
		context.getWorkbenchFrame().addInternalFrame(frame);
		// restore window position and size from Blackboard. We make this after 
		// addInternalFrame, because addInternalFrame calls setLocation..., 
		// so we cannot set location in the ViewAttributesFrame constructor :-(
		int x =  blackboard.get(BB_ATTRIBUTES_WINDOW_POSITION_X, 10);
		int y =  blackboard.get(BB_ATTRIBUTES_WINDOW_POSITION_Y, 10);
		int width =  blackboard.get(BB_ATTRIBUTES_WINDOW_SIZE_WIDTH, 500);
		int height =  blackboard.get(BB_ATTRIBUTES_WINDOW_SIZE_HEIGHT, 300);
		frame.setBounds(x, y, width, height);
		return true;
	}
	public MultiEnableCheck createEnableCheck(
			final WorkbenchContext workbenchContext) {
		EnableCheckFactory checkFactory = new EnableCheckFactory(
				workbenchContext);
		return new MultiEnableCheck().add(
				checkFactory.createTaskWindowMustBeActiveCheck()).add(
				checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
	}
	public ImageIcon getIcon() {
		//return IconLoaderFamFam.icon("table.png");
		return IconLoader.icon("Row.gif");
	}
	public static class ViewAttributesFrame extends DetachableInternalFrame
			implements
				LayerManagerProxy,
				SelectionManagerProxy,
				LayerNamePanelProxy,
				TaskFrameProxy,
				LayerViewPanelProxy {
		private LayerManager layerManager;
		private OneLayerAttributeTab attributeTab;
		public ViewAttributesFrame(final Layer layer, final PlugInContext context) {
			this.layerManager = context.getLayerManager();
			addInternalFrameListener(new InternalFrameAdapter() {
				@Override
				public void internalFrameClosed(InternalFrameEvent e) {
					//Assume that there are no other views on the model [Jon
					// Aquino]
					attributeTab.getModel().dispose();
					
					// save window size and position
					blackboard.put(BB_ATTRIBUTES_WINDOW_SIZE_WIDTH, getSize().width);
					blackboard.put(BB_ATTRIBUTES_WINDOW_SIZE_HEIGHT, getSize().height);
					blackboard.put(BB_ATTRIBUTES_WINDOW_POSITION_X, getLocation().x);
					blackboard.put(BB_ATTRIBUTES_WINDOW_POSITION_Y, getLocation().y);
				}
			});
			setResizable(true);
			setClosable(true);
			setMaximizable(true);
			setIconifiable(true);
			getContentPane().setLayout(new BorderLayout());
			attributeTab = new OneLayerAttributeTab(context
					.getWorkbenchContext(), ((TaskFrameProxy) context
					.getActiveInternalFrame()).getTaskFrame(), this).setLayer(layer);
			addInternalFrameListener(new InternalFrameAdapter() {
				@Override
				public void internalFrameOpened(InternalFrameEvent e) {
					attributeTab.getToolBar().updateEnabledState();
				}
			});
			getContentPane().add(attributeTab, BorderLayout.CENTER);
			updateTitle(attributeTab.getLayer());
			context.getLayerManager().addLayerListener(new LayerListener() {
				public void layerChanged(LayerEvent e) {
					if (attributeTab.getLayer() != null) {
						updateTitle(attributeTab.getLayer());
					}
					// Layer REMOVE [mmichaud 2012-01-05]
					if (e.getType() == LayerEventType.REMOVED) {
					    if (e.getLayerable() == attributeTab.getLayer()) {
					        attributeTab.getModel().dispose();
					        context.getLayerManager().removeLayerListener(this);
					        context.getWorkbenchFrame().removeInternalFrame(ViewAttributesFrame.this);
					        dispose();
					    }
					}
				}
				public void categoryChanged(CategoryEvent e) {
				}
				public void featuresChanged(FeatureEvent e) {
				}
			});
			Assert.isTrue(!(this instanceof CloneableInternalFrame),
					I18N.get("ui.plugin.ViewAttributesPlugIn.there-can-be-no-other-views-on-the-InfoModels"));
		}
		public LayerViewPanel getLayerViewPanel() {
			return getTaskFrame().getLayerViewPanel();
		}
		public LayerManager getLayerManager() {
			return layerManager;
		}
		private void updateTitle(Layer layer) {
			String editView;
			if (layer.isEditable()) {
				editView = I18N.get("ui.plugin.ViewAttributesPlugIn.edit");
			} else {
				editView = I18N.get("ui.plugin.ViewAttributesPlugIn.view");
			}
			
			setTitle(" "+I18N.get("ui.plugin.ViewAttributesPlugIn.attributes")
					+": "+ layer.getName());
		}
		public TaskFrame getTaskFrame() {
			return attributeTab.getTaskFrame();
		}
		public SelectionManager getSelectionManager() {
			return attributeTab.getPanel().getSelectionManager();
		}
		public LayerNamePanel getLayerNamePanel() {
			return attributeTab;
		}
	}
}