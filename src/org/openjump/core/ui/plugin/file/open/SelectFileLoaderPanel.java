package org.openjump.core.ui.plugin.file.open;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;

import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.swing.util.SpringUtilities;

import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class SelectFileLoaderPanel extends JPanel implements WizardPanel {
  private static final long serialVersionUID = -3105562554743126639L;

  public static final String KEY = SelectFileLoaderPanel.class.getName();

  private Map<String, JPanel> extensionPanelMap = new HashMap<String, JPanel>();

  private JPanel mainPanel;

  private Map<File, FileLayerLoader> fileLoaderMap = new HashMap<File, FileLayerLoader>();

  private Set<InputChangedListener> listeners = new LinkedHashSet<InputChangedListener>();

  private OpenFileWizardState state;

  public SelectFileLoaderPanel(final OpenFileWizardState state) {
    super(new BorderLayout());
    this.state = state;
    JPanel scrollPanel = new JPanel(new BorderLayout());

    mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

    scrollPanel.add(mainPanel, BorderLayout.NORTH);
    JScrollPane scrollPane = new JScrollPane(scrollPanel);
    add(scrollPane, BorderLayout.CENTER);

  }

  private void addFiles(final String extension, final Set<URI> files,
    final Set<FileLayerLoader> loaders) {
    final JPanel panel = new JPanel(new SpringLayout());
    panel.setBorder(BorderFactory.createTitledBorder("*." + extension));

    ActionListener useSameListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        panel.removeAll();
        if (((JCheckBox)e.getSource()).isSelected()) {
          addSameSettingsFields(panel, extension, files, loaders, this);
        } else {
          addIndividualSettingsFields(panel, extension, files, loaders, this);
        }
        mainPanel.revalidate();
        mainPanel.repaint();
      }
    };
    addSameSettingsFields(panel, extension, files, loaders, useSameListener);
    mainPanel.add(panel);
  }

  protected void addIndividualSettingsFields(final JPanel panel,
    final String extension, final Set<URI> files,
    final Set<FileLayerLoader> loaders, ActionListener useSameListener) {
    if (files.size() > 1) {
      panel.add(new JLabel("Use same setting for *." + extension));
      JCheckBox useSameField = new JCheckBox();
      useSameField.setSelected(false);
      panel.add(useSameField);
      useSameField.addActionListener(useSameListener);
    }
    for (final URI file : files) {
      panel.add(new JLabel(state.getFileName(file)));

      JComboBox loaderField = new JComboBox();
      for (FileLayerLoader loader : loaders) {
        loaderField.addItem(loader);
      }
      FileLayerLoader selectedLoader = state.getFileLoader(file);
      if (selectedLoader == null) {
        loaderField.setSelectedIndex(0);
        state.setFileLoader(file, loaders.iterator().next());
      } else {
        loaderField.setSelectedItem(selectedLoader);
      }
      loaderField.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          if (e.getStateChange() == ItemEvent.SELECTED) {
            FileLayerLoader fileLayerLoader = (FileLayerLoader)e.getItem();
            state.setFileLoader(file, fileLayerLoader);

            fireInputChanged();
          }
        }
      });
      panel.add(loaderField);
    }

    SpringUtilities.makeCompactGrid(panel, panel.getComponentCount() / 2, 2, 5,
      5, 5, 5);
  }

  protected void addSameSettingsFields(final JPanel panel,
    final String extension, final Set<URI> files,
    final Set<FileLayerLoader> loaders, final ActionListener useSameListener) {
    panel.add(new JLabel("Use same setting for *." + extension));
    JCheckBox useSameField = new JCheckBox();
    useSameField.setSelected(true);
    useSameField.addActionListener(useSameListener);
    panel.add(useSameField);

    panel.add(new JLabel("File Type"));

    JComboBox loaderField = new JComboBox();
    for (FileLayerLoader loader : loaders) {
      loaderField.addItem(loader);
    }
    loaderField.setSelectedIndex(0);
    state.setFileLoader(extension, loaders.iterator().next());
    panel.add(loaderField);
    loaderField.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          FileLayerLoader fileLayerLoader = (FileLayerLoader)e.getItem();
          state.setFileLoader(extension, fileLayerLoader);

          fireInputChanged();
        }
      }
    });
    SpringUtilities.makeCompactGrid(panel, 2, 2, 5, 5, 5, 5);
  }

  public void enteredFromLeft(Map dataMap) {
    mainPanel.removeAll();
    state.setCurrentPanel(SelectFileLoaderPanel.class.getName());
    Map<String, Set<URI>> multiLoaderFiles = state.getMultiLoaderFiles();
    for (Entry<String, Set<URI>> entry : multiLoaderFiles.entrySet()) {
      String extension = entry.getKey();
      Set<URI> extensionFiles = entry.getValue();
      addFiles(extension, extensionFiles, state.getExtensionLoaderMap().get(
        extension));
    }
  }

  public void exitingToRight() throws Exception {
  }

  public String getID() {
    return getClass().getName();
  }

  public String getInstructions() {
    return "Select the type of file for each file extension or individual file";
  }

  public String getNextID() {
    return state.getNextPanel(KEY);
  }

  public String getTitle() {
    return "Select File Types";
  }

  public boolean isInputValid() {
    return true;
  }

  public void add(InputChangedListener listener) {
    listeners.add(listener);
  }

  public void remove(InputChangedListener listener) {
    listeners.remove(listener);
  }

  private void fireInputChanged() {
    for (InputChangedListener listener : listeners) {
      listener.inputChanged();
    }
  }
}
