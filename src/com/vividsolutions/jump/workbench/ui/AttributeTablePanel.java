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
package com.vividsolutions.jump.workbench.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.openjump.core.ui.plugin.view.ViewOptionsPlugIn;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.FlexibleDateParser;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.EditSelectedFeaturePlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

/**
 * Implements an AttributeTable panel. Table-size changes are absorbed by the
 * last column. Rows are striped for non-editable table.
 */

public class AttributeTablePanel extends JPanel implements AttributeTablePanelListener {
	
	/**
	 * The property name of the columns width map in the project file (resides in the data-source subtree).
	 */
	public static final String ATTRIBUTE_COLUMNS_WIDTH_MAP = "AttributeColumnsWidthMap";
    public static final String DATE_FORMAT_KEY = ViewOptionsPlugIn.DATE_FORMAT_KEY;

    private static SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss");

    private Blackboard blackboard;

    ImageIcon nullObject = IconLoader.icon("null1.png");
    ImageIcon nullString = IconLoader.icon("null1.png");

    public static interface FeatureEditor {

        void edit(PlugInContext context, Feature feature, Layer layer)
                throws Exception;

    }

    private FeatureEditor featureEditor = new FeatureEditor() {

        public void edit(PlugInContext context, Feature feature, final Layer myLayer)
                throws Exception {
            new EditSelectedFeaturePlugIn() {

				@Override
                protected Layer layer(PlugInContext context) {
                    //Hopefully nobody will ever delete or rename the
                    // superclass' #layer method.
                    //[Jon Aquino]
                    return myLayer;
                    //Name "myLayer" because we don't want the
                    //superclass' "layer" [Jon Aquino 2004-03-17]
                }
            }.execute(context, feature, myLayer.isEditable());
        }
    };

    private GridBagLayout gridBagLayout1 = new GridBagLayout();

    private class MyTable extends JTable {

        public MyTable() {
            //We want table-size changes to be absorbed by the last column.
            //By default, AUTO_RESIZE_LAST_COLUMN will not achieve this
            //(it works for column-size changes only). But I am overriding
            //#sizeColumnsToFit (for J2SE 1.3) and
            //JTableHeader#getResizingColumn (for J2SE 1.4)
            //#so that it will work for table-size changes. [Jon Aquino]
            setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            GUIUtil.doNotRoundDoubles(this);
            blackboard = PersistentBlackboardPlugIn.get(workbenchContext);
            DateFormat formatter;
            try {
                formatter = blackboard.get(DATE_FORMAT_KEY) == null ?
                        DEFAULT_DATE_FORMAT :
                        new SimpleDateFormat(blackboard.get(DATE_FORMAT_KEY).toString());
            } catch (IllegalArgumentException e) {
                formatter = DEFAULT_DATE_FORMAT;
            }
            //setDefaultEditor(Date.class, new FlexibleDateParser.CellEditor(formatter));

            NullifyMouseAdapter nullifyMouseAdapter = new NullifyMouseAdapter(MyTable.this);

            DefaultCellEditor dateCellEditor = new FlexibleDateParser.CellEditor(formatter);
            // I don't know why it does not work for Date cells
            // It is not a big problem as one can empty the date field to nullify date value
            //dateCellEditor.getComponent().addMouseListener(nullifyMouseAdapter);
            setDefaultEditor(Date.class, dateCellEditor);

            JTextField nullableTextField = new JTextField();
            nullableTextField.addMouseListener(nullifyMouseAdapter);
            setDefaultEditor(String.class, new DefaultCellEditor(nullableTextField));

            JCheckBox nullableCheckBox = new JCheckBox();
            nullableCheckBox.addMouseListener(nullifyMouseAdapter);
            setDefaultEditor(Boolean.class, new DefaultCellEditor(nullableCheckBox));

        }

        //Row-stripe colour recommended in
        //Java Look and Feel Design Guidelines: Advanced Topics [Jon Aquino]
        private final Color LIGHT_GRAY = new Color(230, 230, 230);

        private GeometryCellRenderer geomCellRenderer = new GeometryCellRenderer();
        
		@Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            if (!isEditButtonColumn(column)) {
                final JComponent renderer = (JComponent) super.getCellRenderer(row,
                        column);
                // Get the prefered date formatter from the PersistentBlackboard
                Blackboard blackBoard = PersistentBlackboardPlugIn.get(workbenchContext);
                DateFormat _formatter;
                try {
                    _formatter = blackboard.get(DATE_FORMAT_KEY) == null ?
                            DEFAULT_DATE_FORMAT :
                            new SimpleDateFormat(blackboard.get(DATE_FORMAT_KEY).toString());
                } catch (IllegalArgumentException e) {
                    _formatter = DEFAULT_DATE_FORMAT;
                }
                // We need a final formatter to be used in innerClass
                final DateFormat formatter = _formatter;

                setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                    public void setValue(Object value) {
                        if (value == null) {
                            setIcon(nullObject);
                            setHorizontalAlignment(SwingConstants.CENTER);
                        }
                        else setText(value.toString());
                    }
                });
                setDefaultRenderer(Date.class, new DefaultTableCellRenderer() {
                    public void setValue(Object value) {
                        if (value == null) {
                            setIcon(nullString);
                            setHorizontalAlignment(SwingConstants.CENTER);
                        }
                        else setText(formatter.format(value));
                    }
                });
                setDefaultRenderer(Time.class, new DefaultTableCellRenderer() {
                    public void setValue(Object value) {
                        if (value == null) {
                            setIcon(nullString);
                            setHorizontalAlignment(SwingConstants.CENTER);
                        }
                        else setText(formatter.format(value));
                    }
                });
                setDefaultRenderer(Timestamp.class, new DefaultTableCellRenderer() {
                    public void setValue(Object value) {
                        if (value == null) {
                            setIcon(nullString);
                            setHorizontalAlignment(SwingConstants.CENTER);
                        }
                        else setText(formatter.format(value));
                    }
                });
                // Set default editor here too, as we want date display and date editing
                // to be synchronized
                setDefaultEditor(Date.class, new FlexibleDateParser.CellEditor(formatter));
                setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
                    public void setValue(Object value) {
                        if (value == null) {
                            setIcon(nullString);
                            setHorizontalAlignment(SwingConstants.CENTER);
                        }
                        else setText(value.toString());
                    }
                });
                setDefaultRenderer(Integer.class, new DefaultTableCellRenderer() {
                    public void setValue(Object value) {
                        if (value == null) {
                            setIcon(nullString);
                            setHorizontalAlignment(SwingConstants.CENTER);
                        }
                        else setText(value.toString());
                    }
                });
                setDefaultRenderer(Long.class, new DefaultTableCellRenderer() {
                    public void setValue(Object value) {
                        if (value == null) {
                            setIcon(nullString);
                            setHorizontalAlignment(SwingConstants.CENTER);
                        }
                        else setText(value.toString());
                    }
                });
                setDefaultRenderer(Double.class, new DefaultTableCellRenderer() {
                    public void setValue(Object value) {
                        if (value == null) {
                            setIcon(nullString);
                            setHorizontalAlignment(SwingConstants.CENTER);
                        }
                        else setText(value.toString());
                    }
                });
                setDefaultRenderer(BigDecimal.class, new DefaultTableCellRenderer() {
                    public void setValue(Object value) {
                        if (value == null) {
                            setIcon(nullString);
                            setHorizontalAlignment(SwingConstants.CENTER);
                        }
                        else setText(value.toString());
                    }
                });
                setDefaultRenderer(Boolean.class, new NullableCheckBox());

                if (AttributeTablePanel.this.getModel().getLayer().isEditable()
						&& !AttributeTablePanel.this.getModel()
							.isCellEditable(row, column))
					// Shade readonly cells light gray
					renderer.setBackground(LIGHT_GRAY);
				else {
					// If not editable, use row striping, as recommended in
					// Java Look and Feel Design Guidelines: Advanced Topics
					// [Jon Aquino]
					renderer.setBackground((AttributeTablePanel.this.getModel()
							.getLayer().isEditable() || ((row % 2) == 0)) ? Color.white
							: LIGHT_GRAY);
				}
				return (TableCellRenderer) renderer;
            }
            return geomCellRenderer;
        }
    };

    class NullableCheckBox extends JCheckBox implements TableCellRenderer {
        public NullableCheckBox() {
            super();
            setHorizontalAlignment(SwingConstants.CENTER);
        }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) {
                //this.setBackground(Color.LIGHT_GRAY);
                this.setIcon(nullObject);
            } else {
                super.setSelected((Boolean)value);
            }
            return this;
        }
    }

    // A popup menu item to set a value to null
    class PopUpNullifyCell extends JPopupMenu {
        JMenuItem anItem;
        public PopUpNullifyCell(){
            anItem = new JMenuItem(nullString);
            add(anItem);
        }
    }

    // A mouse listener to display a PopUpNullifyCell
    // The popup is display after a left mouse pressed and disappear after the mouse release
    // To nullify a value, click the PopUpNullifyCell between mouse press and mouse release
    class NullifyMouseAdapter extends MouseAdapter {
        JTable table;
        PopUpNullifyCell menu;

        NullifyMouseAdapter(final JTable table){
            super();
            this.table = table;
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                menu = new PopUpNullifyCell();
                final int row = table.convertRowIndexToModel(table.getEditingRow());
                final int column = table.convertColumnIndexToModel(table.getEditingColumn());
                menu.anItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        table.removeEditor();
                        table.getModel().setValueAt(null, row, column);
                        //System.out.println(row + "/" + column + " : " + table.getModel().getValueAt(row, column));
                    }
                });
                // Wait 1/2 s before displaying the nullify popup menu,
                // so that the normal edition mode is not disturbed
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(333);
                            if (menu!= null) menu.show(e.getComponent(), e.getX() + 10, e.getY());
                        } catch(InterruptedException ie) {}
                    }
                }).start();

            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (menu != null) {
                menu.setVisible(false);
                menu = null;
            }
        }
    };

    private ImageIcon buildPartlyEmptyIcon(ImageIcon icon) {
        ImageIcon empty = buildEmptyIcon(icon);
        // build mask
        BufferedImage mask = new BufferedImage(icon.getIconWidth(),
            icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mask.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, mask.getWidth(), mask.getHeight());
        g.setColor(Color.BLACK);
        g.fillRect(mask.getWidth()/2-1, 0, mask.getWidth(), mask.getHeight());
        // overlay half-red onto normal icon
        icon = GUIUtil.overlay(icon, empty, 0, 0, 1F, mask);
        return icon;
    }
    
    private ImageIcon buildEmptyIcon(ImageIcon icon) {
        ImageIcon gray = GUIUtil.toGrayScale(icon);
        // build red
        BufferedImage red = new BufferedImage(icon.getIconWidth(),
            icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = red.createGraphics();
        g.setColor(Color.PINK);
        g.fillRect(0, 0, red.getWidth(), red.getHeight());
        // build red empty
        ImageIcon empty = GUIUtil.overlay(new ImageIcon(red), gray, 0, 0, 1F, null);
        return empty;
    }
   
    private JLabel buildIconLabel( ImageIcon icon ){
        icon = GUIUtil.pad(icon, 2);
        return new JLabel(icon);
    }
    
   private JLabel buildEmptyIconLabel( ImageIcon icon ){
        icon = GUIUtil.pad(icon, 2);
        JLabel b = buildIconLabel(buildEmptyIcon(icon));
        return b;
   }
   
   private JLabel buildPartlyEmptyIconLabel( ImageIcon icon ){
        icon = GUIUtil.pad(icon, 2);
        JLabel b = buildIconLabel(buildPartlyEmptyIcon(icon));
        return b;
   }
   
    private boolean isPartlyEmpty(Geometry g) {
        if (g.isEmpty())
          return false;

        for (int i = 0; i < g.getNumGeometries(); i++) {
          Geometry inner = g.getGeometryN(i);
          if (inner.isEmpty())
            return true;
        }
        return false;
    }
    
    private class GeometryCellRenderer implements TableCellRenderer {
        private ImageIcon gc = IconLoader.icon("EditGeometryCollection.gif");
        private ImageIcon point = IconLoader.icon("EditPoint.gif");
        private ImageIcon mpoint = IconLoader.icon("EditMultiPoint.gif");
        private ImageIcon line = IconLoader.icon("EditLineString.gif");
        private ImageIcon mline = IconLoader.icon("EditMultiLineString.gif");
        private ImageIcon poly = IconLoader.icon("EditPolygon.gif");
        private ImageIcon mpoly = IconLoader.icon("EditMultiPolygon.gif");
        private ImageIcon lring = IconLoader.icon("EditLinearRing.gif");

        private JLabel buttonPoint = buildIconLabel(point);
        private JLabel buttonMultiPoint = buildIconLabel(mpoint);
        private JLabel buttonLineString = buildIconLabel(line);
        private JLabel buttonMultiLineString = buildIconLabel(mline);
        private JLabel buttonPolygon = buildIconLabel(poly);
        private JLabel buttonMultiPolygon = buildIconLabel(mpoly);
        private JLabel buttonGC = buildIconLabel(gc);
        private JLabel buttonLinearRing = buildIconLabel(lring);

        private JLabel buttonPointEmpty = buildEmptyIconLabel(point);
        private JLabel buttonMultiPointEmpty = buildEmptyIconLabel(mpoint);
        private JLabel buttonLineStringEmpty = buildEmptyIconLabel(line);
        private JLabel buttonMultiLineStringEmpty = buildEmptyIconLabel(mline);
        private JLabel buttonPolygonEmpty = buildEmptyIconLabel(poly);
        private JLabel buttonMultiPolygonEmpty = buildEmptyIconLabel(mpoly);
        private JLabel buttonGCEmpty = buildEmptyIconLabel(gc);
        private JLabel buttonLinearRingEmpty = buildEmptyIconLabel(lring);

        private JLabel buttonMultiPointPartlyEmpty = buildPartlyEmptyIconLabel(mpoint);
        private JLabel buttonMultiLineStringPartlyEmpty = buildPartlyEmptyIconLabel(mline);
        private JLabel buttonMultiPolygonPartlyEmpty = buildPartlyEmptyIconLabel(mpoly);
        private JLabel buttonGCPartlyEmpty = buildPartlyEmptyIconLabel(gc);

        GeometryCellRenderer() {
            String text = I18N.get("ui.AttributeTablePanel.feature.view-edit");
            JLabel[] buttons = new JLabel[] { buttonPoint, buttonMultiPoint,
                buttonLineString, buttonMultiLineString, buttonPolygon,
                buttonMultiPolygon, buttonGC, buttonLinearRing, buttonPointEmpty,
                buttonMultiPointEmpty, buttonLineStringEmpty,
                buttonMultiLineStringEmpty, buttonPolygonEmpty,
                buttonMultiPolygonEmpty, buttonGCEmpty, buttonLinearRingEmpty };
            for (JLabel button : buttons) {
                button.setToolTipText(text);
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
            Feature f = (Feature) value;
            Geometry g = f.getGeometry();

            if (g instanceof com.vividsolutions.jts.geom.LinearRing)
                return g.isEmpty() ? buttonLinearRingEmpty : buttonLinearRing;
            if (g instanceof com.vividsolutions.jts.geom.Point)
                return g.isEmpty() ? buttonPointEmpty : buttonPoint;
            if (g instanceof com.vividsolutions.jts.geom.MultiPoint)
                return g.isEmpty() ? buttonMultiPointEmpty
                  : isPartlyEmpty(g) ? buttonMultiPointPartlyEmpty : buttonMultiPoint;
            if (g instanceof com.vividsolutions.jts.geom.LineString)
                return g.isEmpty() ? buttonLineStringEmpty : buttonLineString;
            if (g instanceof com.vividsolutions.jts.geom.MultiLineString)
                return g.isEmpty() ? buttonMultiLineStringEmpty
                  : isPartlyEmpty(g) ? buttonMultiLineStringPartlyEmpty
                      : buttonMultiLineString;
            if (g instanceof com.vividsolutions.jts.geom.Polygon)
                return g.isEmpty() ? buttonPolygonEmpty : buttonPolygon;
            if (g instanceof com.vividsolutions.jts.geom.MultiPolygon)
                return g.isEmpty() ? buttonMultiPolygonEmpty
                  : isPartlyEmpty(g) ? buttonMultiPolygonPartlyEmpty
                      : buttonMultiPolygon;

            return g.isEmpty() ? buttonGCEmpty
                : isPartlyEmpty(g) ? buttonGCPartlyEmpty : buttonGC;
        }
    }

    private MyTable table;

    private TableCellRenderer headerRenderer;

    private LayerNameRenderer layerListCellRenderer;

    private ArrayList listeners = new ArrayList();

    private WorkbenchContext workbenchContext;
    private Layer layer;
    private HashMap columnsWidthMap;

    private boolean selectionSynchronized = true;

    private void setSelectionSynchronized(boolean selectionSynchronized) {
        this.selectionSynchronized = selectionSynchronized;
    }

    private boolean isSelectionSynchronized() {
        return selectionSynchronized;
    }


    public AttributeTablePanel(final LayerTableModel model, boolean addScrollPane,
            final WorkbenchContext workbenchContext) {
        this(workbenchContext);
        // this panel is exactly for this layer
        this.layer = model.getLayer();
        final SelectionModelWrapper selectionModel = new SelectionModelWrapper(this);
        final DefaultListSelectionModel defaultSelectionModel = new DefaultListSelectionModel();
        table.setSelectionModel(selectionModel);
        selectionModel.setSelectionMode(SelectionModelWrapper.MULTIPLE_INTERVAL_SELECTION);
        selectionModel.setFireSelectionReplaced(true);

        // A LayerViewPanel listener to reflect layerView selection into the AttributeTablePanel
        final LayerViewPanelListener layerViewPanelListener = new LayerViewPanelListener() {
            @Override
            public void selectionChanged() {
                try {
                    // Get selected features :
                    // For AttributeTable, selected features are highlighted
                    // For InfoModel, selected features may be added to the model
                    // before being highlighted
                    Collection selection = workbenchContext
                            .getLayerViewPanel().getSelectionManager()
                            .getFeaturesWithSelectedItems(layer);

                    // From now on (2015-06-13), LayerViewSelection is propagated to
                    // AttributeTablePanel and the other way.
                    // This assertion will avoid recursive updates between LayerView and
                    // AttributeTable
                    selectionModel.setFireSelectionReplaced(false);

                    table.clearSelection();
                    if (selection.size() == 0) {
                        selectionModel.setFireSelectionReplaced(true);
                        return;
                    }

                    // Map feature ids to row ids
                    Map<Integer, Integer> mapIdRow = new HashMap<Integer, Integer>();
                    int rowCount = getModel().getRowCount();
                    for (int row = 0; row < rowCount; row++) {
                        mapIdRow.put(getModel().getFeature(row).getID(), row);
                    }
                    // add selected features which are not yet in the AttributeTablePanel
                    if (selection.size() > 0) {
                        int nextRow = getModel().getRowCount();
                        List<Feature> newFeatures = new ArrayList<Feature>();
                        for (Object obj : selection) {
                            int fid = ((Feature) obj).getID();
                            if (!mapIdRow.containsKey(fid)) {
                                newFeatures.add((Feature) obj);
                            }
                        }
                        getModel().addAll(newFeatures);
                        for (int row = nextRow; row < getModel().getRowCount(); row++) {
                            mapIdRow.put(getModel().getFeature(row).getID(), row);
                        }
                    }

                    // create a set of sorted rows to be selected
                    Set<Integer> rowset = new TreeSet<Integer>();
                    for (Object obj : selection) {
                        rowset.add(mapIdRow.get(((Feature) obj).getID()));
                    }

                    // return if the set of selected raws spread over too man intervals
                    // as it will take to much time to update the table
                    //Integer[] rows = rowset.toArray(new Integer[rowset.size()]);
                    //int countRanges = 0;
                    //for (int i = 1; i < rowset.size(); i++) {
                    //    if (rows[i] - rows[i - 1] > 1) countRanges++;
                    //}

                    // update the table
                    int rowini = -2, rowfin = -2;
                    table.setSelectionModel(defaultSelectionModel);
                    for (int row : rowset) {
                        if (row == rowfin + 1) rowfin = row;
                        else if (row > rowfin + 1) {
                            if (rowfin >= rowini && rowini > -1) {
                                selectionModel.addSelectionInterval(rowini, rowfin);
                            }
                            rowini = row;
                            rowfin = row;
                        }
                    }
                    if (rowfin >= rowini && rowini > -1) {
                        selectionModel.addSelectionInterval(rowini, rowfin);
                    }
                    table.setSelectionModel(selectionModel);

                } finally {
                    selectionModel.setFireSelectionReplaced(true);
                }
            }

            @Override
            public void cursorPositionChanged(String x, String y) {

            }

            @Override
            public void painted(Graphics graphics) {

            }
        };

        if (addScrollPane) {
          remove(table);
          remove(table.getTableHeader());
          JScrollPane scrollPane = new JScrollPane();
          scrollPane.getViewport().add(table);
          this.add(scrollPane, new GridBagConstraints(0, 2, 1, 1, 1, 1,
              GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
                  0, 0), 0, 0));
        }
        updateGrid(model.getLayer());
        model.getLayer().getLayerManager().addLayerListener(
                new LayerListener() {

                    public void categoryChanged(CategoryEvent e) {
                    }

                    public void featuresChanged(FeatureEvent e) {
                    }

                    public void layerChanged(LayerEvent e) {
                        if (e.getLayerable() != model.getLayer()) { return; }
                        if (e.getType() == LayerEventType.METADATA_CHANGED) {
                            //If layer becomes editable, apply row striping
                            // and remove gridlines,
                            //as recommended in Java Look and Feel Design
                            // Guidelines: Advanced Topics [Jon Aquino]
                            updateGrid(model.getLayer());
                            repaint();
                        }
                    }
                });
        try {
            JList list = new JList();
            list.setBackground(new JLabel().getBackground());
            layerListCellRenderer.getListCellRendererComponent(list, model
                    .getLayer(), -1, false, false);
            table.setModel(model);
            model.addTableModelListener(new TableModelListener() {

                public void tableChanged(TableModelEvent e) {
                    if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                        //Structure changed (LayerTableModel specifies
                        // HEADER_ROW).
                        //Add this listener after the table adds its listeners
                        //(in table.setModel above) so that this listener will
                        // initialize the column
                        //widths after the table re-adds the columns. [Jon
                        // Aquino]
                        initColumnWidths();
                    }
                }
            });
            layerListCellRenderer.getLabel().setFont(
                    layerListCellRenderer.getLabel().getFont()
                            .deriveFont(Font.BOLD));
            model.addTableModelListener(new TableModelListener() {

                public void tableChanged(TableModelEvent e) {
                    updateLabel();
                }
            });
            updateLabel();

            table.getTableHeader().setDefaultRenderer(headerRenderer);
            initColumnWidths();
            setToolTips();
            setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0,
                    new FeatureInfoWriter().sidebarColor(model.getLayer())));
            table.getTableHeader().addMouseListener(new MouseAdapter() {

				@Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        int column = table.columnAtPoint(e.getPoint());
                        if (column < 0) { return; }
                        if (isEditButtonColumn(column)) { return; }
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            model.sort(table.getColumnName(column));
                            boolean oldSync = isSelectionSynchronized();
                            //setSelectionSynchronized(true);
                            layerViewPanelListener.selectionChanged();
                            //setSelectionSynchronized(oldSync);
                        }
                    } catch (Throwable t) {
                        workbenchContext.getErrorHandler().handleThrowable(t);
                    }
                }
            });
            table.addMouseListener(new MouseAdapter() {

				@Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        int column = table.columnAtPoint(e.getPoint());
                        int row = table.rowAtPoint(e.getPoint());
                        if (isEditButtonColumn(column)) {
                            PlugInContext context = new PlugInContext(
                                    workbenchContext, null, model.getLayer(),
                                    null, null);
                            model.getLayer().getLayerManager()
                                    .getUndoableEditReceiver().startReceiving();
                            try { 
                                featureEditor.edit(context, model
                                        .getFeature(row), model.getLayer());
                            } finally {
                                model.getLayer().getLayerManager()
                                        .getUndoableEditReceiver()
                                        .stopReceiving();
                            }
                            return;
                        }
                    } catch (Throwable t) {
                        workbenchContext.getErrorHandler().handleThrowable(t);
                    }
                }
            });
            // pressing a key starts the cell editing mode, but it did not 
            // clear the selection and kept delete feature action possible,
            // which was quite dangerous. Now, the selection is cleared
            table.addKeyListener(new java.awt.event.KeyListener(){
                    public void keyPressed(java.awt.event.KeyEvent e) {
                        if (!layer.isEditable()) return;
                        if (e.isControlDown() || e.isAltDown()) return;
                        if (e.getKeyCode() == KeyEvent.VK_DELETE) return;
                        if (e.getKeyCode() == KeyEvent.VK_SHIFT) return; 
                        if (e.getKeyCode() == KeyEvent.VK_CONTROL) return;
                        // if layer is editable and the user pressed a key 
                        // without ctrl or alt pressed. If this key is not
                        // delete or shift or ctrl, the table will enter in 
                        // edition mode. Before that, clear the selection to
                        // avoid the risk to delete the selection while editing
                        table.getSelectionModel().clearSelection();
                    }
                    public void keyReleased(java.awt.event.KeyEvent e) {
                    }
                    public void keyTyped(java.awt.event.KeyEvent e) {
                    }
            });
            // Just after it has been created, AttributeTablePanel listen to the layerView selection
            // to be able to reflect the view selection into the table
            workbenchContext.getLayerViewPanel().addListener(layerViewPanelListener);
            this.addListener(this);
            // reflect the layerView feature selection into the AttributeTablePanel
            // just "after" the table and its model have been initialized
            layerViewPanelListener.selectionChanged();
        } catch (Throwable t) {
            workbenchContext.getErrorHandler().handleThrowable(t);
        }
    }

    private AttributeTablePanel(final WorkbenchContext workbenchContext) {
      layerListCellRenderer = new LayerNameRenderer();
      layerListCellRenderer.setCheckBoxVisible(false);
      layerListCellRenderer.setProgressIconLabelVisible(false);
      this.workbenchContext = workbenchContext;
      blackboard = PersistentBlackboardPlugIn.get(workbenchContext);
      table = new MyTable();
      headerRenderer = new TableCellRenderer() {

          private Icon clearIcon = IconLoader.icon("Clear.gif");

          private Icon downIcon = IconLoader.icon("Down.gif");

          private TableCellRenderer originalRenderer = table.getTableHeader()
                  .getDefaultRenderer();

          private Icon upIcon = IconLoader.icon("Up.gif");

          public Component getTableCellRendererComponent(JTable table,
                                                         Object value, boolean isSelected, boolean hasFocus, int row,
                                                         int column) {
              JLabel label = (JLabel) originalRenderer
                      .getTableCellRendererComponent(table, value, isSelected,
                              hasFocus, row, column);
              if ((getModel().getSortedColumnName() == null)
                      || !getModel().getSortedColumnName().equals(
                      table.getColumnName(column))) {
                  label.setIcon(clearIcon);
              } else if (getModel().isSortAscending()) {
                  label.setIcon(upIcon);
              } else {
                  label.setIcon(downIcon);
              }
              label.setHorizontalTextPosition(SwingConstants.LEFT);
              return label;
          }
      };
      try {
        jbInit();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    private void updateGrid(Layer layer) {
        table.setShowGrid(layer.isEditable());
    }

    private boolean isEditButtonColumn(int column) {
        return getModel().getColumnName(0).equals(table.getColumnName(column));
    }

    private void updateLabel() {//[sstein] change for translation
    	if (getModel().getRowCount() == 1) {
    		 layerListCellRenderer.getLabel().setText(
                    getModel().getLayer().getName() + " ("
                            + getModel().getRowCount() + " "
							+ I18N.get("ui.AttributeTablePanel.feature") + ")");
    	} else {
    		 layerListCellRenderer.getLabel().setText(
                    getModel().getLayer().getName() + " ("
                            + getModel().getRowCount() + " "
                            + I18N.get("ui.AttributeTablePanel.features") + ")");
    	}
    }

    public LayerTableModel getModel() {
        return (LayerTableModel) table.getModel();
    }

    public JTable getTable() {
        return table;
    }

    public void addListener(AttributeTablePanelListener listener) {
        listeners.add(listener);
    }

    void jbInit() throws Exception {
        this.setLayout(gridBagLayout1);
        this.add(layerListCellRenderer, new GridBagConstraints(0, 0, 2, 1, 1.0,
                0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        this.add(table.getTableHeader(), new GridBagConstraints(0, 1, 1, 1, 0,
                0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
         this.add(table, new GridBagConstraints(0, 2, 1, 1, 0, 0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
                        0, 0, 0, 0), 0, 0)); 
    }

    private void initColumnWidths() {
      GUIUtil.chooseGoodColumnWidths(table);
      int editButtonWidth = 16;
      table.getColumnModel().getColumn(0).setMinWidth(editButtonWidth);
      table.getColumnModel().getColumn(0).setMaxWidth(editButtonWidth);
      table.getColumnModel().getColumn(0).setPreferredWidth(editButtonWidth);
  
      // reset to possibly saved column widths
      if (layer.getDataSourceQuery() instanceof DataSourceQuery) {
        HashMap savedWidthMap = (HashMap) layer.getDataSourceQuery()
            .getDataSource().getProperties().get(ATTRIBUTE_COLUMNS_WIDTH_MAP);
        if (savedWidthMap instanceof HashMap)
          columnsWidthMap = savedWidthMap;
      }
      changeColumnWidths(table.getColumnModel(), false);
  
      // add the Listener for changes
      table.getColumnModel().addColumnModelListener(
          new TableColumnModelListener() {
            final MyTable mytable = table;
  
            public void columnAdded(TableColumnModelEvent e) {
              changeColumnWidths((TableColumnModel) e.getSource(), false);
            }
  
            public void columnRemoved(TableColumnModelEvent e) {
              changeColumnWidths((TableColumnModel) e.getSource(), false);
            }
  
            public void columnMoved(TableColumnModelEvent e) {
              changeColumnWidths((TableColumnModel) e.getSource(), false);
            }
  
            public void columnMarginChanged(ChangeEvent e) {
              changeColumnWidths((TableColumnModel) e.getSource(), true);
            }
  
            public void columnSelectionChanged(ListSelectionEvent e) {
              // do nothing
            }
          });
    }
	
    /**
     * This method handle the changes on the TableColumnModel. The width of all
     * columns will be stored in the datasource properties. Later this can be
     * saved within the projectfile.
     * 
     * @param columnModel
     * @param override
     *          ignore datasource defaults, e.g. if user manually resizes col in
     *          gui
     */
    private void changeColumnWidths(TableColumnModel columnModel, boolean override) {
  
      // init col widths memory map
      if (!(columnsWidthMap instanceof HashMap))
        columnsWidthMap = new HashMap(columnModel.getColumnCount());
      // loop over table cols and restore if entry found
      for (int i = 0; i < columnModel.getColumnCount(); i++) {
        Integer savedWidth = (Integer) columnsWidthMap.get(columnModel.getColumn(
            i).getHeaderValue());
        Integer curWidth = columnModel.getColumn(i).getWidth();
        // get or add new entry, override signal user resizes
        if (savedWidth != null && !override)
          columnModel.getColumn(i).setPreferredWidth(savedWidth);
        else
          columnsWidthMap
              .put(columnModel.getColumn(i).getHeaderValue(), curWidth);
      }
  
      // and finaly save the map to the projects properties
      if (layer.getDataSourceQuery() == null)
        return;
      layer.getDataSourceQuery().getDataSource().getProperties()
          .put(ATTRIBUTE_COLUMNS_WIDTH_MAP, columnsWidthMap);
    }

    private void setToolTips() {
        table.addMouseMotionListener(new MouseMotionAdapter() {

			@Override
            public void mouseMoved(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                if (column == -1) { return; }
                table.setToolTipText(table.getColumnName(column) + " ["
                        + getModel().getLayer().getName() + "]");
            }
        });
    }

    /**
     * Called when the user creates a new selection, rather than adding to the
     * existing selection
     */
    private void fireSelectionReplaced() {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            AttributeTablePanelListener listener = (AttributeTablePanelListener) i
                    .next();
            listener.selectionReplaced(this);
        }
    }

    private static class SelectionModelWrapper implements ListSelectionModel {

        private AttributeTablePanel panel;

        private ListSelectionModel selectionModel;

        private boolean fireSelectionReplaced = true;

        public void setFireSelectionReplaced(boolean b) {
            this.fireSelectionReplaced = b;
        }

        public SelectionModelWrapper(AttributeTablePanel panel) {
            this.panel = panel;
            selectionModel = panel.table.getSelectionModel();
        }

        public void setAnchorSelectionIndex(int index) {
            selectionModel.setAnchorSelectionIndex(index);
        }

        public void setLeadSelectionIndex(int index) {
            selectionModel.setLeadSelectionIndex(index);
        }

        public void setSelectionInterval(int index0, int index1) {
            selectionModel.setSelectionInterval(index0, index1);
            if (fireSelectionReplaced) panel.fireSelectionReplaced();
        }

        public void setSelectionMode(int selectionMode) {
            selectionModel.setSelectionMode(selectionMode);
        }

        public void setValueIsAdjusting(boolean valueIsAdjusting) {
            selectionModel.setValueIsAdjusting(valueIsAdjusting);
        }

        public int getAnchorSelectionIndex() {
            return selectionModel.getAnchorSelectionIndex();
        }

        public int getLeadSelectionIndex() {
            return selectionModel.getLeadSelectionIndex();
        }

        public int getMaxSelectionIndex() {
            return selectionModel.getMaxSelectionIndex();
        }

        public int getMinSelectionIndex() {
            return selectionModel.getMinSelectionIndex();
        }

        public int getSelectionMode() {
            return selectionModel.getSelectionMode();
        }

        public boolean getValueIsAdjusting() {
            return selectionModel.getValueIsAdjusting();
        }

        public boolean isSelectedIndex(int index) {
            return selectionModel.isSelectedIndex(index);
        }

        public boolean isSelectionEmpty() {
            return selectionModel.isSelectionEmpty();
        }

        public void addListSelectionListener(ListSelectionListener x) {
            selectionModel.addListSelectionListener(x);
        }

        public void addSelectionInterval(int index0, int index1) {
            selectionModel.addSelectionInterval(index0, index1);
            if (fireSelectionReplaced) panel.fireSelectionReplaced();
        }

        public void clearSelection() {
            selectionModel.clearSelection();
            if (fireSelectionReplaced) panel.fireSelectionReplaced();
        }

        public void insertIndexInterval(int index, int length, boolean before) {
            selectionModel.insertIndexInterval(index, length, before);
            if (fireSelectionReplaced) panel.fireSelectionReplaced();
        }

        public void removeIndexInterval(int index0, int index1) {
            selectionModel.removeIndexInterval(index0, index1);
            if (fireSelectionReplaced) panel.fireSelectionReplaced();
        }

        public void removeListSelectionListener(ListSelectionListener x) {
            selectionModel.removeListSelectionListener(x);
        }

        public void removeSelectionInterval(int index0, int index1) {
            selectionModel.removeSelectionInterval(index0, index1);
            if (fireSelectionReplaced) panel.fireSelectionReplaced();
        }
    }

    public Collection getSelectedFeatures() {
        ArrayList selectedFeatures = new ArrayList();
        if (getModel().getRowCount() == 0) {
        	return selectedFeatures;
        }
        int[] selectedRows = table.getSelectedRows();
        for (int i = 0; i < selectedRows.length; i++) {
            selectedFeatures.add(getModel().getFeature(selectedRows[i]));
        }
        return selectedFeatures;
    }

    public LayerNameRenderer getLayerNameRenderer() {
        return layerListCellRenderer;
    }
    public void setFeatureEditor(FeatureEditor featureEditor) {
        this.featureEditor = featureEditor;
    }

    /**
     * Called by the SelectionModelWrapper to update LayerViewPanel
     * when the table selection is changed.
     * @param panel
     */
    public void selectionReplaced(AttributeTablePanel panel) {
        if (table.isEditing()) table.clearSelection();
        int[] selectedRows = table.getSelectedRows();
        // After selectedRows have been memorized, clear the layer selection,
        // other wise OpenJUMP will add the selectedRows to the already selected features
        workbenchContext.getLayerViewPanel().getSelectionManager().unselectItems(panel.layer);
        ArrayList selectedFeatures = new ArrayList();
        for (int j = 0; j < selectedRows.length; j++) {
            selectedFeatures.add(getModel().getFeature(selectedRows[j]));
        }
        workbenchContext
                .getLayerViewPanel()
                .getSelectionManager()
                .getFeatureSelection()
                .selectItems(panel.layer, selectedFeatures);
    }

}