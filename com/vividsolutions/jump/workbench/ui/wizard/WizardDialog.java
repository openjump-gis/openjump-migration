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

package com.vividsolutions.jump.workbench.ui.wizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import org.openjump.swing.listener.InvokeMethodActionListener;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;

public class WizardDialog extends JDialog implements WizardContext,
  InputChangedListener {
  private List<WizardPanel> completedWizardPanels = new ArrayList<WizardPanel>();

  private JButton cancelButton = new JButton();

  private JButton nextButton = new JButton();

  private JButton backButton = new JButton();

  private JPanel centerPanel = new JPanel();

  private JLabel titleLabel = new JLabel();

  private CardLayout cardLayout = new CardLayout();

  private WizardPanel currentWizardPanel;

  private List<WizardPanel> allWizardPanels = new ArrayList<WizardPanel>();

  private ErrorHandler errorHandler;

  private JTextArea instructionTextArea = new JTextArea();

  private boolean finishPressed = false;

  private HashMap dataMap = new HashMap();

  public WizardDialog(Frame frame, String title, ErrorHandler errorHandler) {
    super(frame, title, true);
    this.errorHandler = errorHandler;

    jbInit();

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        cancel();
      }
    });
  }

  private void checkIDs(Collection wizardPanels) {
    ArrayList ids = new ArrayList();

    for (Iterator i = wizardPanels.iterator(); i.hasNext();) {
      WizardPanel panel = (WizardPanel)i.next();
      ids.add(panel.getID());
    }

    for (Iterator i = wizardPanels.iterator(); i.hasNext();) {
      WizardPanel panel = (WizardPanel)i.next();

      if (panel.getNextID() == null) {
        continue;
      }

      Assert.isTrue(ids.contains(panel.getNextID()),
        I18N.get("ui.wizard.WizardDialog.required-panel-missing") + " "
          + panel.getNextID());
    }
  }

  private void setCurrentWizardPanel(WizardPanel wizardPanel) {
    if (currentWizardPanel != null) {
      currentWizardPanel.remove(this);
    }

    titleLabel.setText(wizardPanel.getTitle());
    cardLayout.show(centerPanel, wizardPanel.getID());
    currentWizardPanel = wizardPanel;
    updateButtons();
    currentWizardPanel.add(this);
    instructionTextArea.setText(currentWizardPanel.getInstructions());
  }

  public WizardPanel setCurrentWizardPanel(String id) {
    WizardPanel panel = find(id);
    panel.enteredFromLeft(dataMap);
    setCurrentWizardPanel(panel);
    return panel;
  }

  private WizardPanel getCurrentWizardPanel() {
    return currentWizardPanel;
  }

  private void updateButtons() {
    backButton.setEnabled(!completedWizardPanels.isEmpty());
    nextButton.setEnabled(getCurrentWizardPanel().isInputValid());
    nextButton.setText((getCurrentWizardPanel().getNextID() == null) ? I18N.get("ui.wizard.WizardDialog.finish")
      : I18N.get("ui.wizard.WizardDialog.next") + " >");
  }

  public void inputChanged() {
    updateButtons();
  }

  /**
   * @param wizardPanels the first of which will be the first WizardPanel that
   *          is displayed
   */
  public void init(WizardPanel[] wizardPanels) {
    List<WizardPanel> panels = Arrays.asList(wizardPanels);
    init(panels);
  }

  protected void setPanels(List<WizardPanel> wizardPanels) {
    allWizardPanels.clear();
    allWizardPanels.addAll(wizardPanels);
    completedWizardPanels.clear();
    checkIDs(allWizardPanels);

    for (WizardPanel wizardPanel : wizardPanels) {
      centerPanel.add((Component)wizardPanel, wizardPanel.getID());
    }
  }

  public void init(List<WizardPanel> wizardPanels) {
    setPanels(wizardPanels);
    WizardPanel firstPanel = wizardPanels.get(0);
    firstPanel.enteredFromLeft(dataMap);
    setCurrentWizardPanel(firstPanel);
    pack();
  }

  private void jbInit() {
    Container contentPane = getContentPane();

    JPanel titlePanel = new JPanel(new GridLayout(2, 1));
    titlePanel.setBackground(Color.white);
    titlePanel.setForeground(Color.black);
    add(titlePanel, BorderLayout.NORTH);

    titleLabel.setFont(new Font("Dialog", 1, 12));
    titleLabel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
    titleLabel.setOpaque(false);
    titleLabel.setText("Title");
    titlePanel.add(titleLabel);

    instructionTextArea.setFont(new JLabel().getFont());
    instructionTextArea.setEnabled(false);
    instructionTextArea.setBorder(BorderFactory.createEmptyBorder(0, 5, 3, 5));
    instructionTextArea.setOpaque(false);
    instructionTextArea.setToolTipText("");
    instructionTextArea.setDisabledTextColor(instructionTextArea.getForeground());
    instructionTextArea.setEditable(false);
    instructionTextArea.setLineWrap(true);
    instructionTextArea.setWrapStyleWord(true);
    instructionTextArea.setText("instructionTextArea");
    titlePanel.add(instructionTextArea);

    centerPanel.setLayout(cardLayout);
    centerPanel.setBorder(BorderFactory.createEtchedBorder(Color.white,
      new Color(148, 145, 140)));
    contentPane.add(centerPanel, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
    contentPane.add(buttonPanel, BorderLayout.SOUTH);

    backButton.setText("< " + I18N.get("ui.wizard.WizardDialog.back"));
    backButton.addActionListener(new InvokeMethodActionListener(this,
      "previous"));
    buttonPanel.add(backButton);

    nextButton.setText(I18N.get("ui.wizard.WizardDialog.next") + " >");
    nextButton.addActionListener(new InvokeMethodActionListener(this, "next"));
    buttonPanel.add(nextButton);

    JLabel spacer = new JLabel();
    spacer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
    buttonPanel.add(spacer);

    cancelButton.setText(I18N.get("ui.wizard.WizardDialog.cancel"));
    cancelButton.addActionListener(new InvokeMethodActionListener(this,
      "cancel"));
    buttonPanel.add(cancelButton);

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
  }

  public void cancel() {
    setVisible(false);
  }

  public void next() {
    try {
      getCurrentWizardPanel().exitingToRight();

      if (getCurrentWizardPanel().getNextID() == null) {
        finishPressed = true;
        setVisible(false);

        return;
      }

      completedWizardPanels.add(getCurrentWizardPanel());

      WizardPanel nextWizardPanel = find(getCurrentWizardPanel().getNextID());
      nextWizardPanel.enteredFromLeft(dataMap);
      setCurrentWizardPanel(nextWizardPanel);
    } catch (CancelNextException e) {
      // This exception is ignored as it is just used as we don't want to modify
      // the exitingToRight method to return false if the panel is not ready
      // to move to the next panel.
    } catch (Throwable x) {
      errorHandler.handleThrowable(x);
    }
  }

  private WizardPanel find(String id) {
    for (Iterator i = allWizardPanels.iterator(); i.hasNext();) {
      WizardPanel wizardPanel = (WizardPanel)i.next();

      if (wizardPanel.getID().equals(id)) {
        return wizardPanel;
      }
    }

    Assert.shouldNeverReachHere();

    return null;
  }

  public boolean wasFinishPressed() {
    return finishPressed;
  }

  public void previous() {
    WizardPanel prevPanel = (WizardPanel)completedWizardPanels.remove(completedWizardPanels.size() - 1);
    setCurrentWizardPanel(prevPanel);

    // Don't init panel if we're going back. [Jon Aquino]
  }

  public void setData(String name, Object value) {
    dataMap.put(name, value);
  }

  public Object getData(String name) {
    return dataMap.get(name);
  }
}
