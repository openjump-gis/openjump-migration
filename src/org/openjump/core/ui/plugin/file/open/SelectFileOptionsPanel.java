package org.openjump.core.ui.plugin.file.open;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;

import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.core.ui.io.file.Option;
import org.openjump.core.ui.swing.FieldComponentFactoryRegistry;
import org.openjump.swing.FieldComponentFactory;
import org.openjump.swing.listener.ValueChangeEvent;
import org.openjump.swing.listener.ValueChangeListener;
import org.openjump.swing.util.SpringUtilities;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class SelectFileOptionsPanel extends JPanel implements WizardPanel {
  private static final long serialVersionUID = -3105562554743126639L;

  public static final String KEY = SelectFileOptionsPanel.class.getName();

  private JPanel mainPanel;

  private Set<InputChangedListener> listeners = new LinkedHashSet<InputChangedListener>();

  private OpenFileWizardState state;

  private WorkbenchContext workbenchContext;

  public SelectFileOptionsPanel(WorkbenchContext workbenchContext,
    OpenFileWizardState state) {
    super(new BorderLayout());
    this.workbenchContext = workbenchContext;
    this.state = state;
    JPanel scrollPanel = new JPanel(new BorderLayout());

    mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

    scrollPanel.add(mainPanel, BorderLayout.NORTH);
    JScrollPane scrollPane = new JScrollPane(scrollPanel);
    add(scrollPane, BorderLayout.CENTER);
  }

  public void enteredFromLeft(Map dataMap) {
    mainPanel.removeAll();
    for (Entry<FileLayerLoader, Set<URI>> entry : state.getFileLoaderFiles()
      .entrySet()) {
      FileLayerLoader fileLayerLoader = entry.getKey();
      List<Option> optionFields = fileLayerLoader.getOptionMetadata();
      if (!optionFields.isEmpty()) {
        Set<URI> files = entry.getValue();
        addLoader(fileLayerLoader, optionFields, files);
      }
    }
  }

  private void addLoader(final FileLayerLoader loader,
    final List<Option> optionFields, final Set<URI> files) {
    final JPanel panel = new JPanel();
    final String description = loader.getDescription();
    panel.setBorder(BorderFactory.createTitledBorder(description));

    ActionListener useSameListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        panel.removeAll();
        if (((JCheckBox)e.getSource()).isSelected()) {
          addSameSettingsFields(loader, panel, description, files,
            optionFields, this);
        } else {
          addIndividualSettingsFields(loader, panel, description, files,
            optionFields, this);
        }
        mainPanel.revalidate();
        mainPanel.repaint();
        fireInputChanged();
      }

    };
    addIndividualSettingsFields(loader, panel, description, files,
      optionFields, useSameListener);
    mainPanel.add(panel);
  }

  public void addIndividualSettingsFields(final FileLayerLoader loader,
    JPanel panel, String description, Set<URI> files,
    List<Option> optionFields, ActionListener useSameListener) {
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    if (files.size() > 1) {
      JPanel samePanel = new JPanel(new SpringLayout());
      samePanel.add(new JLabel("Use same setting for " + description));
      JCheckBox useSameField = new JCheckBox();
      useSameField.setSelected(false);
      samePanel.add(useSameField);
      useSameField.addActionListener(useSameListener);
      SpringUtilities.makeCompactGrid(samePanel, 1, 2, 5, 5, 5, 5);
      samePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
      panel.add(samePanel);
    }
    for (final URI file : files) {
      JPanel filePanel = new JPanel(new SpringLayout());
      Map<String, Object> options = state.getOptions(file);
      filePanel.setBorder(BorderFactory.createTitledBorder(state.getFileName(file)));
      for (Option option : optionFields) {
        final String name = option.getName();
        String label = I18N.get(loader.getClass().getName() + "."
          + name.replaceAll(" ", "-"));
        filePanel.add(new JLabel(name));

        String type = option.getType();
        FieldComponentFactory factory = FieldComponentFactoryRegistry.getFactory(
          workbenchContext, type);
        ValueChangeListener fieldListener = new ValueChangeListener() {
          public void valueChanged(ValueChangeEvent event) {
            Object value = event.getValue();
            state.setOption(file, name, value);
            fireInputChanged();
          }
        };
        JComponent field = factory.createComponent(fieldListener);
        factory.setValue(field, options.get(name));
        filePanel.add(field);
        SpringUtilities.makeCompactGrid(filePanel,
          filePanel.getComponentCount() / 2, 2, 5, 5, 5, 5);

      }
      filePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
      panel.add(filePanel);
    }
  }

  public void addSameSettingsFields(final FileLayerLoader loader, JPanel panel,
    String description, Set<URI> files, List<Option> optionFields,
    ActionListener useSameListener) {
    panel.setLayout(new SpringLayout());
    panel.add(new JLabel("Use same setting for all " + description));
    JCheckBox useSameField = new JCheckBox();
    useSameField.setSelected(true);
    useSameField.addActionListener(useSameListener);
    panel.add(useSameField);

    for (Option option : optionFields) {
      final String label = option.getName();
      panel.add(new JLabel(label));

      String type = option.getType();
      FieldComponentFactory factory = FieldComponentFactoryRegistry.getFactory(
        workbenchContext, type);
      panel.add(factory.createComponent(new ValueChangeListener() {
        public void valueChanged(ValueChangeEvent event) {
          Object value = event.getValue();
          state.setOption(loader, label, value);
          fireInputChanged();
        }
      }));
      state.setOption(loader, option.getName(), null);
    }

    SpringUtilities.makeCompactGrid(panel, 1 + optionFields.size(), 2, 5, 5, 5,
      5);
  }

  public void exitingToRight() throws Exception {
  }

  public String getID() {
    return KEY;
  }

  public String getInstructions() {
    return "Select the type of file for each file type or individual file";
  }

  public String getNextID() {
    return state.getNextPanel(KEY);
  }

  public String getTitle() {
    return "Select File Options";
  }

  public boolean isInputValid() {
    return state.hasRequiredOptions();
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
