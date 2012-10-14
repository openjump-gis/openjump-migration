/*
Copyright (c) 2012, Micha�l Michaud
All rights reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of its authors nor the names of its contributors may
      be used to endorse or promote products derived from this software without
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS "AS IS" AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.openjump.core.ui.plugin.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.*;

import java.util.Collection;

import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.DefaultComboBoxModel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.model.FeatureEventType;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.ObservableFeatureCollection;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.ErrorDialog;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.InfoFrame;
import com.vividsolutions.jump.workbench.ui.DualPaneInputDialog;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.OKCancelPanel;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * Creates a new layer, adding a (dynamic) attribute computed from a
 * beanshell expression.
 * @author Micha&euml;l Michaud
 * @version 0.3 (2012-10-13)
 */
 // 0.3 (2012-10-13) complete refactoring
 //      inclusion of dynamic capabilities in core openjump feature package
 // 0.2 (2011-03-26) fix a NPE
 // 0.1 (2007-08-15)
public class BeanshellAttributeCalculatorPlugIn extends ThreadedBasePlugIn
                                                implements GenericNames {
    
    private static final String KEY = BeanshellAttributeCalculatorPlugIn.class.getName();
    
    private Interpreter interpreter;
    
    //String LAYER                   = I18N.getString(KEY + ".layer");
    private static String BEANSHELL_ATT_CAL       = I18N.get(KEY);
    private static String NEW_ATTRIBUTE_NAME      = I18N.get(KEY + ".new-attribute-name");
    private static String NEW_ATTRIBUTE_TYPE      = I18N.get(KEY + ".new-attribute-type");
    private static String BSH_EXPRESSION          = I18N.get(KEY + ".beanshell-expression");
    private static String SCRIPT_SNIPPETS         = I18N.get(KEY + ".script-snippets");
    private static String COMPUTING_NEW_ATTRIBUTE = I18N.get(KEY + ".computing-new-attribute");
    private static String TOO_MANY_ERRORS         = I18N.get(KEY + ".too-many-errors");
    private static String DYNAMIC                 = I18N.get(KEY + ".dynamic");
    private static String TEST_EXPRESSION         = I18N.get(KEY + ".test-expression");
    private static String SCRIPT_INIT_ERROR       = I18N.get(KEY + ".script-initialisation-error");
    private static String SCRIPT_EVAL_ERROR       = I18N.get(KEY + ".script-evaluation-error");
    private static String SCRIPT_EVAL             = I18N.get(KEY + ".script-evaluation");
    private static String SCRIPT_OK               = I18N.get(KEY + ".script-ok");
    
    String[] FUNCTIONS = new String[]{".trim()",
                                      ".toLowerCase()",
                                      ".toUpperCase()",
                                      ".length()",
                                      "round(double,int)",
                                      "FEATURE.ID",
                                      "GEOMETRY.area",
                                      "GEOMETRY.length",
                                      "GEOMETRY.coordinate.x",
                                      "GEOMETRY.coordinate.y",
                                      "GEOMETRY.coordinate.z",
                                      "GEOMETRY.geometryType",
                                      "GEOMETRY.numPoints",
                                      "GEOMETRY.distance(selection())",
                                      "GEOMETRY.isValid()",
                                      ".matches(?)",
                                      ".replaceAll(?,?)",
                                      ".substring(0,2)",
                                      "()?\"true\":\"false\"",
                                      "?==null?\"0\":\"1\"", };
    
    String src_layer_name;
    String new_attribute_name = NEW_ATTRIBUTE_NAME;
    String bsh_expression;
    AttributeType new_attribute_type;
    boolean dynamic;
    final Vector<String> keywords = new Vector<String>();
    
    public String getName() {
        return BEANSHELL_ATT_CAL;
    }
    
    public void initialize(final PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuItem(
          this, new String[]{MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES},
          getName(),
          false, null, new MultiEnableCheck()
          .add(context.getCheckFactory().createTaskWindowMustBeActiveCheck())
          .add(context.getCheckFactory().createAtLeastNLayersMustExistCheck(1)));
    }
    
    public boolean execute(final PlugInContext context) {
        
        final DualPaneInputDialog dialog = new DualPaneInputDialog (
        context.getWorkbenchFrame(), BEANSHELL_ATT_CAL, true);
        
        Layer src_layer;
        if (src_layer_name == null || 
                context.getLayerManager().getLayer(src_layer_name) == null) {
            src_layer = context.getCandidateLayer(0);
        } 
        else {
            src_layer = context.getLayerManager().getLayer(src_layer_name);
        }
        final JComboBox jcb_layer1 = dialog.addLayerComboBox(
            SOURCE_LAYER, src_layer, null, context.getLayerManager());
        keywords.clear();
        keywords.addAll(getFieldsFromLayer(context.getCandidateLayer(0)));
        Collections.addAll(keywords, FUNCTIONS);
        
        final JTextField jtf_att1 = dialog.addTextField(
            NEW_ATTRIBUTE_NAME, getDefaultAttributeName(new_attribute_name), 16, null, null);
        
        List list = new ArrayList();
        list.add(AttributeType.STRING);
        list.add(AttributeType.DOUBLE);
        list.add(AttributeType.INTEGER);
        list.add(AttributeType.DATE);
        list.add(AttributeType.OBJECT);
        final JComboBox jcb_type = dialog.addComboBox(NEW_ATTRIBUTE_TYPE, AttributeType.STRING, list, "");
        
        final JCheckBox jcb_dynamic = dialog.addCheckBox(
            DYNAMIC, false, DYNAMIC);
        
        final JTextArea jta_bsh_expression = dialog.addTextAreaField(
            BSH_EXPRESSION, "\"Nb Pts = \" + GEOMETRY.getNumPoints()", 3, 50, true, null, BSH_EXPRESSION);
        
        final JButton test_expression = dialog.addButton(TEST_EXPRESSION);
        test_expression.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    checkExpression(context, dialog);
                }
        });

        dialog.setRightPane();
        final JList keywordsList = new JList(keywords);
        keywordsList.setVisibleRowCount(12); 
        final JScrollPane scrollPane = new JScrollPane(keywordsList);
        dialog.addSubTitle(SCRIPT_SNIPPETS);
        dialog.addRow(scrollPane);
        
        keywordsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==2) {
                    jta_bsh_expression.replaceSelection(
                        keywordsList.getSelectedValue().toString());
                }
            }
        });
        
        jcb_layer1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                keywords.clear();
                keywords.addAll(getFieldsFromLayer(dialog.getLayer(SOURCE_LAYER)));
                Collections.addAll(keywords, FUNCTIONS);
                keywordsList.setListData(keywords);
            }
        });
        
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (dialog.wasOKPressed()) {
            src_layer_name = dialog.getLayer(SOURCE_LAYER).getName();
            new_attribute_name = dialog.getText(NEW_ATTRIBUTE_NAME);
            new_attribute_type = (AttributeType)dialog.getComboBox(NEW_ATTRIBUTE_TYPE).getSelectedItem();
            dynamic = dialog.getBoolean(DYNAMIC);
            bsh_expression = dialog.getText(BSH_EXPRESSION);
            try {initInterpreter(context, dialog.getLayer(SOURCE_LAYER));}
            catch(EvalError e) {context.getWorkbenchFrame().warnUser(e.toString());}
            return true;
        }
        else return false;
        
    }
    
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.allowCancellationRequests();
        monitor.report(COMPUTING_NEW_ATTRIBUTE + "...");
        
        FeatureCollection fc = context.getLayerManager().getLayer(src_layer_name).getFeatureCollectionWrapper();
        FeatureSchema fs = fc.getFeatureSchema();
        
        // Expression evaluator
        BeanshellExpression bshExp = new BeanshellExpression(context, interpreter,
                src_layer_name, new_attribute_name, new_attribute_type, bsh_expression);
        
        // Schema of the new layer
        FeatureSchema dfs = (FeatureSchema)fs.clone();
        if (dynamic) {
            dfs.addDynamicAttribute(new_attribute_name, new_attribute_type, bshExp);
        } 
        else {
            dfs.addAttribute(new_attribute_name, new_attribute_type);
        }
        
        FeatureCollection result = new FeatureDataset(dfs);
        int errors = 0;
        EvalError evalError = null;
        for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
            try {
                Feature oldFeature = (Feature)it.next();
                Feature newFeature = new BasicFeature(dfs);
                for (int i = 0 ; i < fs.getAttributeCount() ; i++) {
                    newFeature.setAttribute(fs.getAttributeName(i), oldFeature.getAttribute(i));
                }
                // values are added if the new attribue is not dynamic
                // otherwise, values are evaluated as needed
                if (!dynamic) newFeature.setAttribute(new_attribute_name, bshExp.evaluate((BasicFeature)newFeature));
                result.add(newFeature);
            } 
            catch(EvalError e) {
                throw e;
                //errors++;
                //if (errors > 12) {
                //    context.getWorkbenchFrame().warnUser(TOO_MANY_ERRORS + " (" + e.toString() + ")");
                //    break;
                //}
            }
        }
        context.getLayerManager().addLayer(StandardCategoryNames.RESULT, 
                src_layer_name + "_" + new_attribute_name, result);
    }
    
    /**
     * BeanshellExpression implements FeatureSchema.Operation wich defines
     * how dynamic attributes are evaluated.
     */
    public static class BeanshellExpression implements FeatureSchema.Operation {
        private String bsh_expression;
        private Interpreter interpreter;
        private PlugInContext context;
        private String src_layer_name;
        private String new_attribute_name;
        private AttributeType new_attribute_type;
        public BeanshellExpression(PlugInContext context,
                                   Interpreter interpreter,
                                   String src_layer_name,
                                   String dyn_attribute_name,
                                   AttributeType dyn_attribute_type,
                                   String bsh_expression) {
            this.interpreter = interpreter;
            this.src_layer_name = src_layer_name;
            this.context = context;
            this.new_attribute_name = dyn_attribute_name;
            this.new_attribute_type = dyn_attribute_type;
            this.bsh_expression = bsh_expression;
        }
        public Object invoke(Feature feature) throws Exception {
            return evaluate((BasicFeature)feature);
        }
        public Object evaluate(BasicFeature f) throws EvalError {
            FeatureSchema schema = f.getSchema();
            
            // evaluated dynamic attributes are tagged in the feature userData 
            // to avoid cyclic reference 
            Set<Integer> evaluatedAttributes = (Set<Integer>)f.getUserData("evaluatedAttributes");
            if (evaluatedAttributes == null) {
                evaluatedAttributes = new HashSet<Integer>();
                f.setUserData("evaluatedAttributes", evaluatedAttributes);
            }
            evaluatedAttributes.add(schema.getAttributeCount()-1);
            
            for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
                // to avoid cyclic references, dynamic attributes are evaluated only once
                if (evaluatedAttributes.contains(i)) continue;
                try {
                    interpreter.set(normalizeVarName(
                        schema.getAttributeName(i)), f.getAttribute(i));
                    evaluatedAttributes.add(i);
                }
                catch(EvalError e) {
                    context.getWorkbenchFrame().warnUser(e.toString());
                    throw e;
                }
            }
            f.removeUserData((Object)"evaluatedAttributes");
            try {
                interpreter.set("geometry", f.getGeometry());
                interpreter.set("Geometry", f.getGeometry());
                interpreter.set("GEOMETRY", f.getGeometry());
                interpreter.set("feature", f);
                interpreter.set("Feature", f);
                interpreter.set("FEATURE", f);
                Object obj = interpreter.eval(bsh_expression);
                if (obj == null) return null;
                else if (new_attribute_type == AttributeType.STRING) return obj.toString();
                else if (new_attribute_type == AttributeType.DOUBLE) return new Double(obj.toString());
                else if (new_attribute_type == AttributeType.INTEGER) return new Integer(obj.toString());
                else if (new_attribute_type == AttributeType.DATE) return new Date(obj.toString());
                else return obj;
            }
            catch(EvalError e) {
                context.getWorkbenchFrame().warnUser(e.toString());
                throw e;
            }
        }
    }
    
    
    private void initInterpreter(PlugInContext context, Layer layer) throws EvalError {
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        interpreter = new Interpreter();
        interpreter.setClassLoader(context.getWorkbenchContext().getWorkbench()
                .getPlugInManager().getClassLoader());
        interpreter.set("wc", context.getWorkbenchContext());
        interpreter.eval("setAccessibility(true)");
        interpreter.eval("import com.vividsolutions.jts.geom.*");
        interpreter.eval("import com.vividsolutions.jump.feature.*");
        interpreter.eval("import com.vividsolutions.jts.operation.union.UnaryUnionOp");

        interpreter.set(normalizeVarName(layer.getName()), layer);
        interpreter.eval("selection() {"+
            "return wc.layerViewPanel.selectionManager.featuresWithSelectedItems;}");
        interpreter.eval(
            "dataset(String layerName) {" +
            "return wc.layerManager.getLayer(layerName).getFeatureCollectionWrapper().features;}");
        interpreter.eval("intersects(Feature feature, Collection features) {" +
            "for (f : features) {if (feature.geometry.intersects(f.geometry) && feature.ID!=f.ID) return true;}" +
            "return false;}");
        interpreter.eval("distance(Feature feature, Collection features) {" +
            "min = Double.MAX_VALUE;" +
            "for (f : features) {min = Math.min(min,feature.geometry.distance(f.geometry));}" +
            "return min == Double.MAX_VALUE ? null : min;}");
        interpreter.eval(
            "round(double d, int i) {" +
            " p10 = Math.pow(10.0,(double)i);" +
            " return Math.rint(d*p10)/p10;" +
            "}");
    }
    
    private static String normalizeVarName(String s) {
        StringBuffer sb = new StringBuffer(s);
        for (int i = 0 ; i < s.length() ; i++) {
            if (!Character.isJavaIdentifierPart(sb.charAt(i))) {
                sb.setCharAt(i, '_');
            }
        }
        return sb.toString();
    }
    
    private List getFieldsFromLayer(Layer l) {
        List fields = new ArrayList();
        FeatureSchema schema = l.getFeatureCollectionWrapper().getFeatureSchema();
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
           fields.add(normalizeVarName(schema.getAttributeName(i)));  
        }
        return fields;
    }
    
    private String getDefaultAttributeName(String base) {
        if (base.startsWith(NEW_ATTRIBUTE_NAME)) {
            String ext = base.substring(NEW_ATTRIBUTE_NAME.length());
            if (ext.trim().length() == 0) return NEW_ATTRIBUTE_NAME + "1";
            else if (ext.matches("[1-9][0-9]*")) {
                return NEW_ATTRIBUTE_NAME + (Integer.parseInt(ext)+1);
            }
            else return base;
        }
        else return NEW_ATTRIBUTE_NAME;
    }
    
    /**
     * Evaluate the beanshellExpression against the few first features or 
     * against a fake feature if the selected Layer is empty
     */
    private void checkExpression(final PlugInContext context, final DualPaneInputDialog dialog) {
        src_layer_name = dialog.getLayer(SOURCE_LAYER).getName();
        new_attribute_name = dialog.getText(NEW_ATTRIBUTE_NAME);
        new_attribute_type = (AttributeType)dialog.getComboBox(NEW_ATTRIBUTE_TYPE).getSelectedItem();
        bsh_expression = dialog.getText(BSH_EXPRESSION);
        try {
            initInterpreter(context, dialog.getLayer(SOURCE_LAYER));
        }
        catch(EvalError e) {
            ErrorDialog.show(dialog, SCRIPT_INIT_ERROR, e.toString(), StringUtil.stackTrace(e));
            return;
        }
        FeatureCollection fc = context.getLayerManager().getLayer(src_layer_name).getFeatureCollectionWrapper();
        
        // Expression evaluator
        BeanshellExpression bshExp = new BeanshellExpression(context, interpreter,
                src_layer_name, new_attribute_name, new_attribute_type, bsh_expression);

        int count = 0;
        try {
            for (Iterator it = fc.iterator() ; it.hasNext() ; ) {
                bshExp.evaluate((BasicFeature)it.next());
                if (count++ > 6) return;
            }
            if (count == 0) {
                bshExp.evaluate(createFakeFeature(fc.getFeatureSchema()));
            }
        } catch(EvalError e) {
            ErrorDialog.show(dialog, SCRIPT_EVAL_ERROR, e.toString(), StringUtil.stackTrace(e));
            return;
        }
        
        final JDialog okDialog = new JDialog(dialog, SCRIPT_EVAL, true);
        okDialog.getContentPane().setLayout(new BorderLayout(20,10));
        
        final OKCancelPanel okPanel = new OKCancelPanel();
        okPanel.setOKPressed(true);
        okPanel.getSelectedButton().addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    okDialog.setVisible(false);
                }
        });
        okPanel.setCancelVisible(false);
        okDialog.getContentPane().add(new JLabel(SCRIPT_OK), BorderLayout.CENTER);
        okDialog.getContentPane().add(okPanel, BorderLayout.SOUTH);
        okDialog.getContentPane().add(new JLabel(), BorderLayout.NORTH);
        okDialog.getContentPane().add(new JLabel(), BorderLayout.EAST);
        okDialog.getContentPane().add(new JLabel(), BorderLayout.WEST);
        GUIUtil.centreOnWindow(okDialog);
        okDialog.show();
    }
    
    // Creates a fake feature from a FeatureSchema for script evaluation
    private BasicFeature createFakeFeature(FeatureSchema fs) {
        BasicFeature bf = new BasicFeature(fs);
        for (int i = 0 ; i < fs.getAttributeCount() ; i++) {
            if (fs.isOperation(i)) continue;
            else if (fs.getAttributeType(i) == AttributeType.STRING) bf.setAttribute(i,"Micha�l");
            else if (fs.getAttributeType(i) == AttributeType.DOUBLE) bf.setAttribute(i,9.999);
            else if (fs.getAttributeType(i) == AttributeType.INTEGER) bf.setAttribute(i,100);
            else if (fs.getAttributeType(i) == AttributeType.DATE) bf.setAttribute(i,new Date());
            else if (fs.getAttributeType(i) == AttributeType.GEOMETRY) {
                bf.setAttribute(i, new GeometryFactory().createPoint((Coordinate)null));
            }
            else;
        }
        return bf;
    }

}
