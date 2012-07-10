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

package com.vividsolutions.jump.workbench.ui.toolbox;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.WorkbenchToolBar;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;

/**
 * An always-on-top modeless dialog with a WorkbenchToolBar (for CursorTools,
 * PlugIns, and other buttons). Takes care of unpressing the CursorTools (if
 * necessary) when the dialog is closed (and pressing the first CursorTool on
 * the main toolbar).
 */

//Must use modeless dialog rather than JInternalFrame; otherwise, if we
// implement
//it as an internal frame, when we click it, the original TaskFrame will
// deactivate,
//thus deactivating the current CursorTool. [Jon Aquino]
public class ToolboxDialog extends JDialog {
    public AbstractButton getButton(Class cursorToolClass) {
        for (Iterator i = toolBars.iterator(); i.hasNext();) {
            WorkbenchToolBar toolBar = (WorkbenchToolBar) i.next();
            AbstractButton button = toolBar.getButton(cursorToolClass);
            if (button != null) {
                return button;
            }
        }
        return null;
    }

    public WorkbenchToolBar getToolBar() {
        if (toolBars.isEmpty()) {
            addToolBar();
        }
        return (WorkbenchToolBar) toolBars.get(toolBars.size() - 1);
    }

    public WorkbenchContext getContext() {
        return context;
    }

    public WorkbenchToolBar.ToolConfig add(CursorTool tool) {
        return add(tool, null);
    }

    /**
     * @param enableCheck
     *            null to leave unspecified
     */
    public WorkbenchToolBar.ToolConfig add(CursorTool tool,
            EnableCheck enableCheck) {
        WorkbenchToolBar.ToolConfig config = getToolBar().addCursorTool(tool);
        JToggleButton button = config.getButton();
        getToolBar().setEnableCheck(button,
                enableCheck != null ? enableCheck : new MultiEnableCheck());
        registerButton(button, enableCheck);
        return config;
    }

    public void addPlugIn(PlugIn plugIn, EnableCheck enableCheck, Icon icon) {
        registerButton(getToolBar().addPlugIn(icon, plugIn, enableCheck,
                context), enableCheck);
    }

    private void registerButton(AbstractButton button, EnableCheck enableCheck) {
        buttons.add(button);
    }

    private ArrayList buttons = new ArrayList();

    private ArrayList toolBars = new ArrayList();

    public void addToolBar() {
        toolBars.add(new WorkbenchToolBar(context, context.getWorkbench()
                .getFrame().getToolBar().getButtonGroup()));
        getToolBar().setBorder(null);
        getToolBar().setFloatable(false);
        gridLayout1.setRows(toolBars.size());
        toolbarsPanel.add(getToolBar());
    }

    public ToolboxDialog(final WorkbenchContext context) {
        super(context.getWorkbench().getFrame(), "", false);
        this.context = context;
        super.setVisible(false);
        setResizable(true);
        // [ede 07.2012] remove titlebar to allow packing dialog to width of containing buttons only
        setUndecorated(true);
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        this.addComponentListener(new ComponentAdapter() {
            public void componentHidden(ComponentEvent e) {
                if (buttons.contains(context.getWorkbench().getFrame()
                        .getToolBar().getSelectedCursorToolButton())) {
                    ((AbstractButton) context.getWorkbench().getFrame()
                            .getToolBar().getButtonGroup().getElements()
                            .nextElement()).doClick();
                }
            }
        });
    }

    /**
     * Call this method after all the CursorTools have been added.
     */
    public void finishAddingComponents() {
        pack();
        // #initializeLocation will be called again in #setVisible, but
        // call it here just in case the window is realized by some other
        // means than #setVisible (unlikely). [Jon Aquino 2005-03-14]
        //initializeLocation();
    }

    public void setVisible(boolean visible) {
        if (visible && !locationInitializedBeforeMakingDialogVisible) {
            // #initializeLocation was called in #finishAddingComponents,
            // but the Workbench may have moved since then, so call
            // #initializeLocation again just before making the dialog
            // visible. [Jon Aquino 2005-03-14]
            initializeLocation();
            Dimension dim = new Dimension(toolbarsPanel.getPreferredSize().width,titlePanel.getPreferredSize().height);
            title.setMaximumSize(dim);
            title.setPreferredSize(dim);
            titlePanel.setMaximumSize(dim);
            titlePanel.setPreferredSize(dim);
            pack();
            locationInitializedBeforeMakingDialogVisible = true;
        }
        super.setVisible(visible);
    }

    private boolean locationInitializedBeforeMakingDialogVisible = false;

    private void initializeLocation() {
        GUIUtil.setLocation(this, initialLocation, context.getWorkbench()
                .getFrame().getDesktopPane());
    }

    private GUIUtil.Location initialLocation = new GUIUtil.Location(0, false,
            0, false);

    private WorkbenchContext context;

    private BorderLayout borderLayout1 = new BorderLayout();

    private JPanel centerPanel = new JPanel();

    private BorderLayout borderLayout2 = new BorderLayout();

    private JPanel toolbarsPanel = new JPanel();

    private GridLayout gridLayout1 = new GridLayout(1, 1, 0, 0);

    private JPanel titlePanel = new JPanel();

    private JLabel title = new JLabel("Toolbox", JLabel.CENTER);

    private void jbInit() throws Exception {
        this.getContentPane().setLayout(borderLayout1);
        centerPanel.setLayout(borderLayout2);
        centerPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED)/*new LineBorder(Color.BLACK)*/);
        toolbarsPanel.setLayout(gridLayout1);
        gridLayout1.setColumns(1);
        // setup top titlebar label
        //title.setForeground(Color.WHITE);
        title.setHorizontalTextPosition(JLabel.CENTER);
        title.setBorder(BorderFactory.createEmptyBorder(4,2,4,2));
        title.setToolTipText(I18N.get("com.vividsolutions.jump.workbench.ui.toolbox.click-n-drag-to-move-doubleclick-to-hide"));
        MouseAdapter mover = new MoveCloser(this);
        title.addMouseListener(mover);
        title.addMouseMotionListener(mover);
        // add bottom line to title
        titlePanel.setLayout(new BorderLayout());
        titlePanel.add(title);
        titlePanel.setBackground(Color.lightGray);
        titlePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
        // add components
        centerPanel.add(titlePanel, BorderLayout.NORTH);
        centerPanel.add(toolbarsPanel, BorderLayout.CENTER);

        JPanel borderPanel = new JPanel(new BorderLayout());
        borderPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        borderPanel.add(centerPanel);
        this.getContentPane().add(borderPanel, BorderLayout.CENTER);
    }

    public void setTitle(String text) {
      super.setTitle(text);
      title.setText(text);
      pack();
    }

    public JPanel getCenterPanel() {
        return centerPanel;
    }

    public void updateEnabledState() {
        for (Iterator i = toolBars.iterator(); i.hasNext();) {
            WorkbenchToolBar toolBar = (WorkbenchToolBar) i.next();
            toolBar.updateEnabledState();
        }
    }

    public void setInitialLocation(GUIUtil.Location location) {
        initialLocation = location;
    }

}

class MoveCloser extends MouseAdapter {

  private int offX = 0;
  private int offY = 0;
  private Component cp;

  public MoveCloser(Component cp) {
    this.cp = cp;
  }

  public void mouseClicked(MouseEvent e) {
    if (e.getClickCount() > 1)
      cp.setVisible(false);
  }

  public void mousePressed(MouseEvent e) {
    offX = e.getX();
    offY = e.getY();
  }

  public void mouseDragged(MouseEvent e) {
    int newX = cp.getX() + e.getX() - offX;
    int newY = cp.getY() + e.getY() - offY;
    cp.setLocation(newX, newY);
  }

}