/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2008 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */
package org.openjump.core.ui.plugin.tools;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.AttributeTypeFilter;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

import org.openjump.core.ui.plugin.AbstractUiPlugIn;

/**
* Based on CalculateAreasAndLengthsPlugIn.
*
*/
public class AutoAssignAttributePlugIn extends AbstractUiPlugIn {
    
    private static String LAYER_COMBO_BOX = GenericNames.LAYER;
    private static String SELECTED_CHECK_BOX;
    private static String SELECT_ONLY_ON_ONE_LAYER;
    private static String TARGET_ATTRIBUTE_COMBO_BOX;
    
    private static String AUTOINC_CHECK_BOX;
    private static String INC_VALUE_EDIT_BOX;
    private static String AUTOINC_PATTERN_BOX;
    private static String AUTOINC_DESCRIPTION_1;
    private static String AUTOINC_DESCRIPTION_2;
    
    private static String FROM_SOURCE_CHECK_BOX;
    private static String SOURCE_COMBO_BOX;
    private static String FROM_SOURCE_DESCRIPTION;
    
    private static String ASSIGN_VALUE_CHECK_BOX;
    private static String ASSIGN_VALUE_TEXT_BOX;
    private static String ASSIGN_VALUE_DESCRIPTION;
    
    private static String DESCRIPTION;
    
    private Layer layer;
    private boolean selectedFeaturesOnly = true;
    private String targetAttribute;
    private AttributeType destinationAttributeType;
    
    private boolean autoIncrement = false;
    private String pattern = "0";
    private int autoInc = 1;
    private int incValue;
    
    private String numeric;
    
    private boolean assignFromSource = false;
    private String sourceAttribute;
    
    private boolean assignValue = false;
    private String textToAssign;
	    
	public void initialize(PlugInContext context) throws Exception {
	    
	    context.getFeatureInstaller().addMainMenuItem(
	    	  new String[] { MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES}, 
	    	  this,
	    	  createEnableCheck(context.getWorkbenchContext()));
	      
        SELECTED_CHECK_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Selected-features-only");
        TARGET_ATTRIBUTE_COMBO_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Target-attribute");
        
        SOURCE_COMBO_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Source-attribute");
        FROM_SOURCE_CHECK_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Assign-from-other-attribute");
        FROM_SOURCE_DESCRIPTION = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.From-source-description");
        
        AUTOINC_CHECK_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Auto-increment");
        AUTOINC_PATTERN_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Auto-increment-pattern");
        INC_VALUE_EDIT_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Increment-by");
        AUTOINC_DESCRIPTION_1 = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Auto-increment-description-1");
        AUTOINC_DESCRIPTION_2 = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Auto-increment-description-2");
        
        ASSIGN_VALUE_CHECK_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Assign-fixed-value");
        ASSIGN_VALUE_TEXT_BOX = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Assign-value");
        ASSIGN_VALUE_DESCRIPTION = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Assign-value-description");
        
        SELECT_ONLY_ON_ONE_LAYER = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Select-features-on-only-one-layer");
        DESCRIPTION = I18N.get("org.openjump.core.ui.plugin.tools.AutoAssignAttributePlugIn.Description");
	}
	
	public boolean execute(PlugInContext context) throws Exception {
		MultiInputDialog dialog = prompt(context);
		GUIUtil.centreOnWindow(dialog);
		dialog.setVisible(true);
		if (!dialog.wasOKPressed()) {
			return false;
		}
		getDialogValues(dialog);
		assignValues(context);
		return true;
	}
	
    private MultiInputDialog prompt(PlugInContext context) {
        
        final MultiInputDialog dialog =
            new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
    	dialog.setSideBarDescription(DESCRIPTION);
        
    	// Source layer and target attribute
    	final JComboBox layerComboBox = dialog.addEditableLayerComboBox(
                        LAYER_COMBO_BOX, 
                        context.getLayerNamePanel().chooseEditableLayer(), 
                        null, context.getLayerManager());
        boolean selectionExists = context.getLayerViewPanel()
                                         .getSelectionManager()
                                         .getFeaturesWithSelectedItems()
                                         .size() > 0;
        if (!selectionExists) selectedFeaturesOnly = false;
        final JCheckBox selectedFeaturesOnlyCheckBox = 
            dialog.addCheckBox(SELECTED_CHECK_BOX, selectedFeaturesOnly);
        dialog.setFieldVisible(SELECTED_CHECK_BOX, selectionExists);
        final JComboBox targetAttributeComboBox = 
            dialog.addAttributeComboBox(TARGET_ATTRIBUTE_COMBO_BOX, LAYER_COMBO_BOX,
                                        AttributeTypeFilter.NUMSTRING_FILTER, 
                                        "");
        for (int i = 0 ; i < targetAttributeComboBox.getModel().getSize() ; i++) {
            Object item = targetAttributeComboBox.getModel().getElementAt(i);
            if (item.equals(targetAttribute)) targetAttributeComboBox.setSelectedIndex(i);
        }
        
        // Auto-increment options
        dialog.addSeparator();
        final JCheckBox autoIncCheckBox = dialog.addCheckBox(AUTOINC_CHECK_BOX, autoIncrement);
        final JTextField autoIncPatternField = dialog.addTextField(AUTOINC_PATTERN_BOX, pattern, 4, null, AUTOINC_DESCRIPTION_2);
        final JTextField incField = dialog.addIntegerField(INC_VALUE_EDIT_BOX, 1, 4, "");
        
        // From other attribute option
        dialog.addSeparator();
        final JCheckBox fromSourceCheckBox = dialog.addCheckBox(FROM_SOURCE_CHECK_BOX, assignFromSource);
        final JComboBox sourceAttributeComboBox = 
            dialog.addAttributeComboBox(SOURCE_COMBO_BOX, LAYER_COMBO_BOX,
                                        AttributeTypeFilter.ALL_FILTER, 
                                        "");
        for (int i = 0 ; i < sourceAttributeComboBox.getModel().getSize() ; i++) {
            Object item = sourceAttributeComboBox.getModel().getElementAt(i);
            if (item.equals(sourceAttribute)) sourceAttributeComboBox.setSelectedIndex(i);
        }

        initEnableChecks(dialog);
        
        dialog.addSeparator();
        final JCheckBox assignValueCheckBox = dialog.addCheckBox(ASSIGN_VALUE_CHECK_BOX, assignValue);
        dialog.addTextField(ASSIGN_VALUE_TEXT_BOX, "", 15, null, "");
        
        updateControls(dialog);
        
        autoIncCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(dialog);
            }
        });
        fromSourceCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(dialog);
            }
        });
        assignValueCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(dialog);
            }
        });

        return dialog;
    }
    
    private void updateControls(MultiInputDialog dialog) {
        assignFromSource = dialog.getCheckBox(FROM_SOURCE_CHECK_BOX).isSelected();
        autoIncrement = dialog.getBoolean(AUTOINC_CHECK_BOX);
        assignValue = dialog.getBoolean(ASSIGN_VALUE_CHECK_BOX);
        layer = dialog.getLayer(LAYER_COMBO_BOX);
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        boolean fromAttributeValid = schema.getAttributeCount() > 2;
        
        dialog.setFieldEnabled(AUTOINC_CHECK_BOX, !assignFromSource && !assignValue);
        dialog.setFieldEnabled(AUTOINC_PATTERN_BOX, autoIncrement && !assignFromSource && !assignValue);
        dialog.setFieldEnabled(INC_VALUE_EDIT_BOX, autoIncrement && !assignFromSource && !assignValue);
        
        dialog.setFieldEnabled(FROM_SOURCE_CHECK_BOX, !autoIncrement && !assignValue && fromAttributeValid);
        dialog.setFieldEnabled(SOURCE_COMBO_BOX, assignFromSource && !autoIncrement && !assignValue && fromAttributeValid);
        
        dialog.setFieldEnabled(ASSIGN_VALUE_CHECK_BOX, !assignFromSource && !autoIncrement);
        dialog.setFieldEnabled(ASSIGN_VALUE_TEXT_BOX, assignValue && !assignFromSource && !autoIncrement);
        
        if (assignValue) dialog.setSideBarDescription(ASSIGN_VALUE_DESCRIPTION);
        else if (autoIncrement) dialog.setSideBarDescription(AUTOINC_DESCRIPTION_1 + "\n\n" + AUTOINC_DESCRIPTION_2);
        else if (assignFromSource) dialog.setSideBarDescription(FROM_SOURCE_DESCRIPTION);
        else dialog.setSideBarDescription(DESCRIPTION);
    }
    
    private void getDialogValues(MultiInputDialog dialog) {
		layer = dialog.getLayer(LAYER_COMBO_BOX);
		selectedFeaturesOnly = dialog.getBoolean(SELECTED_CHECK_BOX);
		FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
		targetAttribute = dialog.getText(TARGET_ATTRIBUTE_COMBO_BOX);
		destinationAttributeType = schema.getAttributeType(schema.getAttributeIndex(targetAttribute));
		
		autoIncrement = dialog.getBoolean(AUTOINC_CHECK_BOX);
		incValue = dialog.getInteger(INC_VALUE_EDIT_BOX);
	    pattern = dialog.getText(AUTOINC_PATTERN_BOX);
	    numeric = parseNumber(pattern);
	    
	    assignFromSource = dialog.getBoolean(FROM_SOURCE_CHECK_BOX);
		sourceAttribute = dialog.getText(SOURCE_COMBO_BOX);
        
	    assignValue = dialog.getBoolean(ASSIGN_VALUE_CHECK_BOX);
		textToAssign = dialog.getText(ASSIGN_VALUE_TEXT_BOX);
		
		if (autoIncrement) {
			if (numeric.length() == 0)
				autoInc = 0;
			else
				autoInc = new Integer(numeric).intValue();
		} else
	    	autoInc = 0;
    }
    
    private void initEnableChecks(final MultiInputDialog dialog) {
        dialog.addEnableChecks(SOURCE_COMBO_BOX,
            Arrays.asList(new Object[] {new EnableCheck() {
                public String check(JComponent component) {
                    return assignFromSource && 
                           dialog.getText(TARGET_ATTRIBUTE_COMBO_BOX)
                                 .equals(dialog.getText(SOURCE_COMBO_BOX)) ? 
                                 "Source and destination attributes must be different" : 
                                 null;
                }
            }
        }));
    }
    
    private String attributeName(List attributeNames, int preferredIndex) {
        return (String) attributeNames.get(
            attributeNames.size() > preferredIndex ? preferredIndex : 0);
    }
    
    private Layer candidateLayer(PlugInContext context) {
        if (context.getActiveInternalFrame() instanceof LayerNamePanelProxy) {
            Layer[] selectedLayers = context.getSelectedLayers();
            for (int i = 0; i < selectedLayers.length; i++) {
                if (selectedLayers[i].isEditable()) {
                    return selectedLayers[i];
                }
            }
        }
        return (Layer) context.getLayerManager().getEditableLayers().iterator().next();
    }

    private static interface Converter {
        public Object convert(String d);
    }
    
    private Map typeToConverterMap = new HashMap() {
        {
            put(AttributeType.STRING, new Converter() {
                public Object convert(String d) {
                    return d;
                }
            });
            put(AttributeType.INTEGER, new Converter() {
                public Object convert(String d) {
                    if (d==null) return null;
                	String s = parseNumber(d);
                	if (s.length() == 0) 
                		return new Integer(0);
                    return new Integer(s);
                }
            });
            put(AttributeType.DOUBLE, new Converter() {
                public Object convert(String d) {
                    if (d==null) return null;
                	String s = parseNumber(d);
                	if (s.length() == 0) 
                		return new Double(0);
                    return new Double(parseNumber(d));
                }
            });
        }
    };
 
    private String parseNumber(String text) {
        int b=0; int e=0;
    	for (int i=0; i<text.length(); i++) {
    		if (Character.isDigit(text.charAt(i))) {
		        b=i; e=i;
		   		while ( e < text.length() && Character.isDigit(text.charAt(e))) 
		   			e++;
		   		return text.substring(b, e);
    		}
    	}
    	return "";
    }
     
    private void assignValues(PlugInContext context) {
    	Iterator iterator;
    	if (selectedFeaturesOnly) {
    		Collection layers = context.getLayerViewPanel().getSelectionManager()
    		.getLayersWithSelectedItems();
    		if (layers.size() > 1) {
    			context.getWorkbenchFrame().warnUser(SELECT_ONLY_ON_ONE_LAYER);
    		}
    		iterator = context.getLayerViewPanel().getSelectionManager()
    		.getFeaturesWithSelectedItems().iterator();
    	} else {
    		iterator = layer.getFeatureCollectionWrapper().getFeatures().iterator();
    	}
    	for (Iterator i = iterator; i.hasNext(); ) {
    		Feature feature = (Feature) i.next();
    		String s = textToAssign;
    		if (autoIncrement) {
    		    s = pattern;
    			String value = "" + autoInc;
    			autoInc += incValue;
    			if (numeric.length() == 0)
    				s = value;
    			else
    				s = pattern.replaceFirst(numeric, value);
    		} else if (assignFromSource) {
                s = feature.getAttribute(sourceAttribute).toString();
            } else {
                s = textToAssign;
            }
    		Object object = ((Converter) typeToConverterMap.get(destinationAttributeType)).convert(s);
    		feature.setAttribute(targetAttribute, object);

    	}
    }

    public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
            .add(checkFactory.createAtLeastNLayersMustBeEditableCheck(1));
    }
}
