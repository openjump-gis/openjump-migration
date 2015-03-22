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
package com.vividsolutions.jump.workbench.ui.plugin;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.util.Assert;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.util.FlexibleDateParser;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.text.DateFormat;

import java.util.*;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;


public class ViewSchemaPlugIn extends AbstractPlugIn {
    
    private static final String KEY = ViewSchemaPlugIn.class + " - FRAME";
    private EditingPlugIn editingPlugIn;
    private GeometryFactory factory = new GeometryFactory();
    private WKTReader wktReader = new WKTReader(factory);
    private FlexibleDateParser dateParser = new FlexibleDateParser();
    private DateFormat dateFormatter = DateFormat.getDateInstance();

    private static final String P_LAYER_NAME = "LayerName";
    private static final String P_SCHEMA_MAPPING = "SchemaMapping";
    private static final String P_FORCE_INVALID_CONVERSIONS_TO_NULL = "ForceInvalidConversionsToNull";

    public ViewSchemaPlugIn(EditingPlugIn editingPlugIn) {
        this.editingPlugIn = editingPlugIn;
    }

    public ViewSchemaPlugIn() {
      this.editingPlugIn = EditingPlugIn.getInstance();
    }
    
    public String getName() {
        return I18N.get("ui.plugin.ViewSchemaPlugIn.view-edit-schema");
    }

    private void applyChanges(final Layer layer, final SchemaPanel panel, final WorkbenchFrame workbenchFrame)
        throws Exception {
        if (!panel.isModified()) {
            //User just pressed the Apply button even though he made no edits.
            //Don't truncate the undo history; instead, exit. [Jon Aquino]
            return;
        }

        if (panel.validateInput() != null) {
            throw new Exception(panel.validateInput());
        }

        panel.getModel().removeBlankRows();

        // If the schema is modified, features of the layer are changed,
        // the corresponding attributeTab in the InfoFrame is emptied and
        // the columns are schrinked to a null width.
        // Removing the attributeTab in the InfoFrame avoid these side effects
        for (JInternalFrame iFrame : workbenchFrame.getInternalFrames()) {
            if (iFrame instanceof InfoFrame) ((InfoFrame)iFrame).getModel().remove(layer);
        }

        FeatureSchema newSchema = new FeatureSchema();
        //-- [sstein 10. Oct 2006] bugfix for colortheming by Ole
        FeatureSchema oldSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        //-- end

        for (int i = 0; i < panel.getModel().getRowCount(); i++) {
            //-- [sstein 10. Oct 2006] bugfix for colortheming by Ole
            String attributeName = panel.getModel().get(i).getName();
            newSchema.addAttribute(attributeName, panel.getModel().get(i).getType());
            if (oldSchema.hasAttribute(attributeName)) {
                // [mmichaud - 2012-10-13]
                if (newSchema.getAttributeType(attributeName)
                    .equals(oldSchema.getAttributeType(attributeName))) {
                    newSchema.setAttributeReadOnly(
                        newSchema.getAttributeIndex(attributeName),
                        oldSchema.isAttributeReadOnly(oldSchema.getAttributeIndex(attributeName))
                    );
                    newSchema.setOperation(
                        newSchema.getAttributeIndex(attributeName),
                        oldSchema.getOperation(oldSchema.getAttributeIndex(attributeName))
                    );
                }
                else {
                    if (ColorThemingStyle.get(layer) != null) {
                        layer.removeStyle(ColorThemingStyle.get(layer));
                        layer.getBasicStyle().setEnabled(true);
                        layer.fireAppearanceChanged();
                    }
                }
            }
            //-- END: added/modyfied by Ole
        }

        List originalFeatures = layer.getFeatureCollectionWrapper().getFeatures();
        ArrayList tempFeatures = new ArrayList();

        //Two-phase commit. 
        //Phase 1: check that no conversion errors occur. [Jon Aquino]
        for (Iterator i = layer.getFeatureCollectionWrapper().iterator();
                i.hasNext();) {
            Feature feature = (Feature) i.next();
            tempFeatures.add(convert(feature, panel, newSchema));
        }

        //Phase 2: commit. [Jon Aquino]
        for (int i = 0; i < originalFeatures.size(); i++) {
            Feature originalFeature = (Feature) originalFeatures.get(i);
            Feature tempFeature = (Feature) tempFeatures.get(i);

            //Modify existing features rather than creating new features, because
            //there may be references to the existing features (e.g. Attribute Viewers).
            //[Jon Aquino]            
            originalFeature.setSchema(tempFeature.getSchema());
            originalFeature.setAttributes(tempFeature.getAttributes());
        }

        //Non-undoable. [Jon Aquino]
        layer.getLayerManager().getUndoableEditReceiver().getUndoManager()
             .discardAllEdits();
        layer.setFeatureCollection(new FeatureDataset(originalFeatures,
                newSchema));
        layer.fireLayerChanged(LayerEventType.METADATA_CHANGED);

        // [mmichaud 2014-10-05] add parameters to persist plugin execution in a macro
        addParameter(P_FORCE_INVALID_CONVERSIONS_TO_NULL, panel.isForcingInvalidConversionsToNull());
        Map<String,Attribute> schemaMapping = new LinkedHashMap<String, Attribute>();
        for (int i = 0; i < panel.getModel().getRowCount(); i++) {
            String attributeName = panel.getModel().get(i).getName();
            Attribute attribute = new Attribute();
            attribute.setType(panel.getModel().get(i).getType());
            if (oldSchema.hasAttribute(attributeName)) {
                int oldIndex = oldSchema.getAttributeIndex(attributeName);
                attribute.setReadOnly(oldSchema.isAttributeReadOnly(oldIndex));
            }
            attribute.setOldIndex(panel.getModel().get(i).getOriginalIndex());
            schemaMapping.put(attributeName, attribute);
        }
        addParameter(P_SCHEMA_MAPPING, schemaMapping);
        //

        // [mmichaud 2009-05-16] update originalIndexes after a modification
        for (int i = 0; i < panel.getModel().getRowCount(); i++) {
            panel.getModel().get(i).setOriginalIndex(i);
        }
        // -end
        panel.markAsUnmodified();

    }

    /**
     * Version of applyChanges used when the plugin is executed as a macro
     * @TODO ideally, the same code should be executed in both cases (DRY)
     * @param layer
     * @param schemaMapping
     * @param isForcingInvalidConversionsToNull
     * @throws Exception
     */
    private void applyChanges(final Layer layer, final Map<String,Attribute> schemaMapping,
                              final boolean isForcingInvalidConversionsToNull) throws Exception {

        //-- [sstein 10. Oct 2006] bugfix for colortheming by Ole
        FeatureSchema oldSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        //-- end

        for (String name : schemaMapping.keySet()) {
            if (oldSchema.hasAttribute(name)) {
                if (!schemaMapping.get(name).getType().equals(oldSchema.getAttributeType(name))) {
                    if (ColorThemingStyle.get(layer) != null) {
                        layer.removeStyle(ColorThemingStyle.get(layer));
                        layer.getBasicStyle().setEnabled(true);
                        layer.fireAppearanceChanged();
                    }
                }
            }
        }

        FeatureSchema newSchema = new FeatureSchema();
        for (String name : schemaMapping.keySet()) {
            newSchema.addAttribute(name, schemaMapping.get(name).getType());
            newSchema.setAttributeReadOnly(newSchema.getAttributeIndex(name), schemaMapping.get(name).isReadOnly());
        }

        List originalFeatures = layer.getFeatureCollectionWrapper().getFeatures();
        ArrayList tempFeatures = new ArrayList();

        //Two-phase commit.
        //Phase 1: check that no conversion errors occur. [Jon Aquino]
        for (Iterator i = layer.getFeatureCollectionWrapper().iterator();
             i.hasNext();) {
            Feature feature = (Feature) i.next();
            tempFeatures.add(convert(feature, schemaMapping, newSchema, isForcingInvalidConversionsToNull));
        }

        //Phase 2: commit. [Jon Aquino]
        for (int i = 0; i < originalFeatures.size(); i++) {
            Feature originalFeature = (Feature) originalFeatures.get(i);
            Feature tempFeature = (Feature) tempFeatures.get(i);

            //Modify existing features rather than creating new features, because
            //there may be references to the existing features (e.g. Attribute Viewers).
            //[Jon Aquino]
            originalFeature.setSchema(tempFeature.getSchema());
            originalFeature.setAttributes(tempFeature.getAttributes());
        }

        //Non-undoable. [Jon Aquino]
        layer.getLayerManager().getUndoableEditReceiver().getUndoManager()
                .discardAllEdits();
        layer.setFeatureCollection(new FeatureDataset(originalFeatures,
                newSchema));
        layer.fireLayerChanged(LayerEventType.METADATA_CHANGED);
    }

    private Feature convert(Feature oldFeature, SchemaPanel panel,
        FeatureSchema newSchema) throws ConversionException {
        Feature newFeature = new BasicFeature(newSchema);

        for (int i = 0; i < panel.getModel().getRowCount(); i++) {
            if (panel.getModel().get(i).getOriginalIndex() == -1) {
                newFeature.setAttribute(i,
                    (panel.getModel().get(i).getType() == AttributeType.GEOMETRY)
                    ? oldFeature.getGeometry() : null);
            } else {
                newFeature.setAttribute(i,
                    convert(oldFeature.getAttribute(panel.getModel().get(i)
                                                         .getOriginalIndex()),
                        oldFeature.getSchema().getAttributeType(panel.getModel()
                                                                     .get(i)
                                                                     .getOriginalIndex()),
                        newFeature.getSchema().getAttributeType(i),
                        panel.getModel().get(i).getName(),
                        panel.isForcingInvalidConversionsToNull()));
            }
        }

        return newFeature;
    }

    /**
     * Conversion tool used by macro (no dependence to SchemaPanel).
     * @param oldFeature
     * @param schemaMapping
     * @param newSchema
     * @return
     * @throws ConversionException
     */
    private Feature convert(Feature oldFeature, Map<String,Attribute> schemaMapping, FeatureSchema newSchema,
                            boolean isForcingInvalidConversionsToNull) throws ConversionException {
        Feature newFeature = new BasicFeature(newSchema);

        for (String name : schemaMapping.keySet()) {
            if (schemaMapping.get(name).getOldIndex() == -1) {
                newFeature.setAttribute(name,
                        (newSchema.getAttributeType(name) == AttributeType.GEOMETRY)
                                ? oldFeature.getGeometry() : null);
            } else {
                newFeature.setAttribute(name,
                        convert(oldFeature.getAttribute(schemaMapping.get(name).getOldIndex()),
                                oldFeature.getSchema().getAttributeType(schemaMapping.get(name).getOldIndex()),
                                newFeature.getSchema().getAttributeType(name),
                                name,
                                isForcingInvalidConversionsToNull));
            }
        }

        return newFeature;
    }

    private String limitLength(String s) {
        //Limit length of values reported in error messages -- WKT is potentially large.
        //[Jon Aquino]
        return StringUtil.limitLength(s, 30);
    }

    Pattern TRUE_PATTERN = Pattern.compile("(?i)^(T(rue)?|Y(es)?|V(rai)?|1)$");
    Pattern FALSE_PATTERN = Pattern.compile("(?i)^(F(alse)?|N(o)?|F(aux)?|0)$");

    private Object convert(Object oldValue, AttributeType oldType,
        AttributeType newType, String name,
        boolean forcingInvalidConversionsToNull) throws ConversionException {
        try {
            if (oldValue == null) {
                return (newType == AttributeType.GEOMETRY)
                ? factory.createPoint((Coordinate)null) : null;
            }

            if (oldType == AttributeType.STRING) {
                String oldString = oldValue!=null ? oldValue.toString() : "";

                if (newType == AttributeType.STRING) {
                    return oldString;
                }

                if (newType == AttributeType.INTEGER) {
                    try {
                        return new Integer(oldString.replaceAll("^0*",""));
                    } catch (NumberFormatException e) {
                        throw new ConversionException(
                            I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-integer")+" \"" +
                            limitLength(oldValue.toString()) + "\" (" + name +
                            ")");
                    }
                }

                if (newType == AttributeType.LONG) {
                    try {
                        return new Long(oldString.replaceAll("^0*",""));
                    } catch (NumberFormatException e) {
                        throw new ConversionException(
                                I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-long")+" \"" +
                                        limitLength(oldValue.toString()) + "\" (" + name +
                                        ")");
                    }
                }

                if (newType == AttributeType.DOUBLE) {
                    try {
                        return new Double(oldString);
                    } catch (NumberFormatException e) {
                        throw new ConversionException(
                            I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-double")+" \"" +
                            limitLength(oldValue.toString()) + "\" (" + name +
                            ")");
                    }
                }

                if (newType == AttributeType.BOOLEAN) {
                    if (FALSE_PATTERN.matcher(oldString).matches()) return Boolean.FALSE;
                    else if (TRUE_PATTERN.matcher(oldString).matches()) return Boolean.TRUE;
                    throw new ConversionException(
                            I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-boolean")+" \"" +
                                        limitLength(oldValue.toString()) + "\" (" + name +
                                        ")");
                }

                if (newType == AttributeType.GEOMETRY) {
                    try {
                        return wktReader.read(oldString);
                    } catch (ParseException e) {
                        throw new ConversionException(
                            I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-geometry")+" \"" +
                            limitLength(oldValue.toString()) + "\" (" + name +
                            ")");
                    }
                }

                if (newType == AttributeType.DATE) {
                    try {
                        return dateParser.parse(oldString, false);
                    } catch (java.text.ParseException e) {
                        throw new ConversionException(
                            I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-date")+" \"" +
                            limitLength(oldValue.toString()) + "\" (" + name +
                            ")");
                    }
                }
            }

            if (oldType == AttributeType.INTEGER) {
                int oldInt = ((Integer) oldValue).intValue();

                if (newType == AttributeType.STRING) {
                    return "" + oldInt;
                }

                if (newType == AttributeType.INTEGER) {
                    return oldValue;
                }

                if (newType == AttributeType.LONG) {
                    return new Long(oldInt);
                }

                if (newType == AttributeType.DOUBLE) {
                    return new Double(oldInt);
                }

                if (newType == AttributeType.BOOLEAN) {
                    return oldInt == 0 ? Boolean.FALSE : Boolean.TRUE;
                }

                if (newType == AttributeType.GEOMETRY) {
                    throw new ConversionException(
                        I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-geometry")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.DATE) {
                    try {
                        return dateParser.parse("" + oldInt, false);
                    } catch (java.text.ParseException e) {
                        throw new ConversionException(
                            I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-date")+" \"" +
                            limitLength(oldValue.toString()) + "\" (" + name +
                            ")");
                    }
                }
            }

            if (oldType == AttributeType.DOUBLE) {
                double oldDouble = ((Double) oldValue).doubleValue();

                if (newType == AttributeType.STRING) {
                    return "" + oldDouble;
                }

                if (newType == AttributeType.INTEGER) {
                    return new Integer((int) oldDouble);
                }

                if (newType == AttributeType.LONG) {
                    return new Long((long) oldDouble);
                }

                if (newType == AttributeType.DOUBLE) {
                    return oldValue;
                }

                if (newType == AttributeType.BOOLEAN) {
                    return oldDouble == 0.0 ? Boolean.FALSE : Boolean.TRUE;
                }

                if (newType == AttributeType.GEOMETRY) {
                    throw new ConversionException(
                        I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-geometry")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.DATE) {
                    throw new ConversionException(I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-date")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }
            }

            if (oldType == AttributeType.GEOMETRY) {
                Geometry oldGeometry = (Geometry) oldValue;

                if (newType == AttributeType.STRING) {
                    return oldGeometry.toString();
                }

                if (newType == AttributeType.INTEGER) {
                    throw new ConversionException(
                            I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-integer")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.LONG) {
                    throw new ConversionException(
                            I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-long")+" \"" +
                                    limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.DOUBLE) {
                    throw new ConversionException(
                        I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-double")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.BOOLEAN) {
                    throw new ConversionException(
                            I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-boolean")+" \"" +
                                    limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.GEOMETRY) {
                    return oldGeometry;
                }

                if (newType == AttributeType.DATE) {
                    throw new ConversionException(I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-date")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }
            }

            if (oldType == AttributeType.DATE) {
                Date oldDate = (Date) oldValue;

                if (newType == AttributeType.STRING) {
                    return dateFormatter.format(oldDate);
                }

                if (newType == AttributeType.INTEGER) {
                    return new Integer((int) oldDate.getTime());
                }

                if (newType == AttributeType.LONG) {
                    return oldDate.getTime();
                }

                if (newType == AttributeType.DOUBLE) {
                    return new Double(oldDate.getTime());
                }

                if (newType == AttributeType.BOOLEAN) {
                    throw new ConversionException(
                            I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-boolean")+" \"" +
                                    limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.GEOMETRY) {
                    throw new ConversionException(
                        I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-geometry")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.DATE) {
                    return oldValue;
                }
            }
            
            // [mmichaud 2010-01-29] AttributeType.OBJECT case added
            if (oldType == AttributeType.OBJECT) {
                if (newType == AttributeType.STRING) {
                    return oldValue.toString();
                }

                if (newType == AttributeType.INTEGER) {
                    if (oldValue instanceof Number) {
                        return new Integer(((Number)oldValue).intValue());
                    }
                    throw new ConversionException(
                            I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-integer")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.LONG) {
                    if (oldValue instanceof Number) {
                        return new Long(((Number)oldValue).longValue());
                    }
                    throw new ConversionException(
                            I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-integer")+" \"" +
                                    limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.DOUBLE) {
                    if (oldValue instanceof Number) {
                        return new Double(((Number)oldValue).doubleValue());
                    }
                    throw new ConversionException(
                            I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-double")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.BOOLEAN) {
                    if (oldValue instanceof Boolean) {
                        return oldValue;
                    } else if (oldValue instanceof Number) {
                        return ((Number)oldValue).intValue() == 0 ? Boolean.FALSE : Boolean.TRUE;
                    } else if (oldValue instanceof String) {
                        if (FALSE_PATTERN.matcher(oldValue.toString()).matches()) return Boolean.FALSE;
                        if (TRUE_PATTERN.matcher(oldValue.toString()).matches()) return Boolean.TRUE;
                    }
                    throw new ConversionException(
                            I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-boolean")+" \"" +
                                    limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.GEOMETRY) {
                    if (oldValue instanceof Geometry) return oldValue;
                    throw new ConversionException(
                        I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-geometry")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.DATE) {
                    if (oldValue instanceof Date) return oldValue;
                    throw new ConversionException(
                        I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-date")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }
            }
            
            if (newType == AttributeType.OBJECT) {
                return oldValue;
            }
            // end mmichaud

            Assert.shouldNeverReachHere(newType.toString());

            return null;
        } catch (ConversionException e) {
            if (forcingInvalidConversionsToNull) {
                return (newType == AttributeType.GEOMETRY)
                ? factory.createPoint((Coordinate)null) : null;
            }

            throw e;
        }
    }

    private void commitEditsInProgress(final SchemaPanel panel) {
        //Skip if nothing is being edited, otherwise may get false positive. [Jon Aquino]
        if (panel.getTable().getEditingRow() != -1) {
            //If user is in the middle of editing a field name, call stopCellEditing
            //so that new field name is committed (if valid) or an error is recorded
            //(if invalid). [Jon Aquino]
            panel.getTable()
                 .getCellEditor(panel.getTable().getEditingRow(),
                panel.getTable().getEditingColumn()).stopCellEditing();
        }
    }

    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);

        // [mmichaud 2014-10-05] had to test if we are in macro mode or in interactive mode
        // maybe a better way vould be that all plugins implement a non interactive method
        // called either by execute method (interactive mode) or directly by the RunMacro
        if (!context.getWorkbenchContext().getBlackboard().getBoolean(MacroManager.MACRO_RUNNING)) {
            //Can't simply use Blackboard#get(key, default) because default requires that
            //we create a new EditSchemaFrame, and we don't want to do this unless we
            //have to because the EditSchemaFrame constructor modifies the blackboard.
            //Result: Executing this plug-in twice creates two frames, even if we don't close
            //the first. [Jon Aquino]
            if (frame(context) == null) {
                addParameter(P_LAYER_NAME, context.getSelectedLayer(0).getName());
                context.getSelectedLayer(0).getBlackboard().put(KEY,
                        new EditSchemaFrame(context.getWorkbenchFrame(),
                                context.getSelectedLayer(0), editingPlugIn));
            }

            frame(context).surface();

            if (context.getWorkbenchContext().getBlackboard().getBoolean(MacroManager.MACRO_STARTED)) {
                ((Macro) context.getWorkbenchContext().getBlackboard().get(MacroManager.MACRO)).addProcess(this);
            }
        }
        else {
            // in macro mode, the layer to which the new schema will be applied is choosen in the following order :
            // - if one layer is selected : apply to the selected layer
            // - if several layers are selected : apply to P_LAYER_NAME
            //   or to the first selected layer if P_LAYER_NAME is not selected
            // - if no layer are selected : apply to P_LAYER_NAME or do not apply
            Layer layer;
            Layer[] selectedLayers = context.getLayerNamePanel().getSelectedLayers();
            if (selectedLayers.length == 1) {
                layer = selectedLayers[0];
            } else if (context.getLayerManager().getLayer((String)getParameter(P_LAYER_NAME)) != null) {
                layer = context.getLayerManager().getLayer((String)getParameter(P_LAYER_NAME));
            } else return false;
            applyChanges(layer,
                    (Map<String,Attribute>)getParameter(P_SCHEMA_MAPPING),
                    getBooleanParam(P_FORCE_INVALID_CONVERSIONS_TO_NULL));
        }

        return true;
    }

    private EditSchemaFrame frame(PlugInContext context) {
        return (EditSchemaFrame) context.getSelectedLayer(0).getBlackboard()
                                        .get(KEY);
    }

    public static MultiEnableCheck createEnableCheck(
        final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                                     .add(checkFactory.createExactlyNLayersMustBeSelectedCheck(
                1));
    }

    private static class ConversionException extends Exception {
        public ConversionException(String message) {
            super(message);
        }
    }
    
    public static final ImageIcon ICON = IconLoader.icon("Object.gif");

    private class EditSchemaFrame extends JInternalFrame
        implements LayerNamePanelProxy, LayerNamePanel, LayerManagerProxy {
        private LayerManager layerManager;
        private Layer layer;
        private WorkbenchFrame workbenchFrame;

        public EditSchemaFrame(final WorkbenchFrame workbenchFrame,
            final Layer layer, EditingPlugIn editingPlugIn) {
            this.layer = layer;
            this.workbenchFrame = workbenchFrame;
            layer.getBlackboard().put(KEY, this);

            this.layerManager = layer.getLayerManager();
            addInternalFrameListener(new InternalFrameAdapter() {
                    public void internalFrameClosed(InternalFrameEvent e) {
                        layer.getBlackboard().put(KEY, null);
                    }
                });

            final SchemaPanel panel = new SchemaPanel(layer, editingPlugIn,
                    workbenchFrame.getContext());
            setResizable(true);
            setClosable(true);
            setMaximizable(true);
            setIconifiable(true);
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(panel, BorderLayout.CENTER);
            setSize(500, 300);
            updateTitle(layer);
            layer.getLayerManager().addLayerListener(new LayerListener() {
                    public void categoryChanged(CategoryEvent e) {
                    }

                    public void featuresChanged(FeatureEvent e) {
                    }

                    public void layerChanged(LayerEvent e) {
                        updateTitle(layer);
                    }
                });
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            panel.add(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            commitEditsInProgress(panel);
                            applyChanges(layer, panel, workbenchFrame);
                        } catch (Exception x) {
                            workbenchFrame.handleThrowable(x);
                        }
                    }
                });
            addInternalFrameListener(new InternalFrameAdapter() {
                    public void internalFrameClosing(InternalFrameEvent e) {
                        commitEditsInProgress(panel);

                        if (!layer.isEditable() || !panel.isModified()) {
                            dispose();

                            return;
                        }

                        switch (JOptionPane.showConfirmDialog(EditSchemaFrame.this,
                            I18N.get("ui.plugin.ViewSchemaPlugIn.apply-changes-to-schema"), "JUMP",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE)) {
                        case JOptionPane.YES_OPTION:

                            try {
                                applyChanges(layer, panel, workbenchFrame);
                            } catch (Exception x) {
                                workbenchFrame.handleThrowable(x);
                                return;
                            }

                            dispose();

                            return;

                        case JOptionPane.NO_OPTION:
                            dispose();

                            return;

                        case JOptionPane.CANCEL_OPTION:
                            return;

                        default:
                            Assert.shouldNeverReachHere();
                        }
                    }
                });
        }

        private void updateTitle(Layer layer) {
            setTitle((layer.isEditable() ? 
                    I18N.get("ui.plugin.ViewSchemaPlugIn.edit") : 
                    I18N.get("ui.plugin.ViewSchemaPlugIn.view")) 
                    + " "+I18N.get("ui.plugin.ViewSchemaPlugIn.schema")+": " +
                layer.getName());
        }

        public LayerManager getLayerManager() {
            return layerManager;
        }

        public Layer chooseEditableLayer() {
            return TreeLayerNamePanel.chooseEditableLayer(this);
        }

        public void surface() {
            if (!workbenchFrame.hasInternalFrame(this)) {
                workbenchFrame.addInternalFrame(this, false, true);
            }

            workbenchFrame.activateFrame(this);
            moveToFront();
        }

        public LayerNamePanel getLayerNamePanel() {
            return this;
        }

        public Collection getSelectedCategories() {
            return new ArrayList();
        }

        public Layer[] getSelectedLayers() {
            return new Layer[] { layer };
        }

        public Collection selectedNodes(Class c) {
            if (!Layerable.class.isAssignableFrom(c)) {
                return new ArrayList();
            }

            return Arrays.asList(getSelectedLayers());
        }

        public void addListener(LayerNamePanelListener listener) {}
        public void removeListener(LayerNamePanelListener listener) {}
    }

    public static class ToNewSchema {

        public ToNewSchema(SchemaPanel panel) {

        }
    }

    public static class Attribute {

        AttributeType type;
        boolean readOnly;
        boolean primaryKey;
        Operation operation;
        int oldIndex;

        public Attribute(){}

        public void setType(AttributeType type) {this.type = type;}
        public AttributeType getType() {return type;}

        public void setReadOnly(boolean readOnly) {this.readOnly = readOnly;}
        public boolean isReadOnly() {return readOnly;}

        public void setPrimaryKey(boolean primaryKey) {this.primaryKey = primaryKey;}
        public boolean isPrimaryKey() {return primaryKey;}

        public void setOperation(Operation operation) {this.operation = operation;}
        public Operation getOperation() {return operation;}

        public void setOldIndex(int oldIndex) {this.oldIndex = oldIndex;}
        public int getOldIndex() {return oldIndex;}
    }
}
