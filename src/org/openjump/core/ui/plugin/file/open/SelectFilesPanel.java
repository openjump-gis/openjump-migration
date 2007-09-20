package org.openjump.core.ui.plugin.file.open;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.core.ui.io.file.FileLayerLoaderExtensionFilter;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;

import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.LoadFileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class SelectFilesPanel extends JFileChooser implements WizardPanel {

  public static final String KEY = SelectFilesPanel.class.getName();

  private Set<InputChangedListener> listeners = new LinkedHashSet<InputChangedListener>();

  private Blackboard blackboard;

  private OpenFileWizardState state;

  public SelectFilesPanel(final WorkbenchContext context,
    final OpenFileWizardState state) {
    this.state = state;
    blackboard = PersistentBlackboardPlugIn.get(context);
    Registry registry = context.getRegistry();

    String savedDirectoryName = (String)blackboard.get(LoadFileDataSourceQueryChooser.FILE_CHOOSER_DIRECTORY_KEY);
    if (savedDirectoryName != null) {
      setCurrentDirectory(new File(savedDirectoryName));
    }

    setAcceptAllFileFilterUsed(false);
    setMultiSelectionEnabled(true);
    List loaders = registry.getEntries(FileLayerLoader.KEY);
    Set<String> allExtensions = new TreeSet<String>();
    allExtensions.add("zip");
    allExtensions.add("gz");
    Map<String, FileFilter> filters = new TreeMap<String, FileFilter>();
    for (Object loader : loaders) {
      final FileLayerLoader fileLayerLoader = (FileLayerLoader)loader;
      FileFilter filter = new FileLayerLoaderExtensionFilter(fileLayerLoader);
      allExtensions.addAll(fileLayerLoader.getFileExtensions());
      filters.put(filter.getDescription(), filter);
    }

    FileFilter allFilter = new FileNameExtensionFilter("All Files",
      allExtensions.toArray(new String[0]));
    addChoosableFileFilter(allFilter);
    for (FileFilter filter : filters.values()) {
      addChoosableFileFilter(filter);

    }
    setFileFilter(allFilter);

    setControlButtonsAreShown(false);

    addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        FileLayerLoader fileLayerLoader = null;
        File[] files = getSelectedFiles();
        FileFilter selectedFileFilter = getFileFilter();
        if (selectedFileFilter instanceof FileLayerLoaderExtensionFilter) {
          FileLayerLoaderExtensionFilter filter = (FileLayerLoaderExtensionFilter)selectedFileFilter;
          fileLayerLoader = filter.getFileLoader();
        }
        state.setupFileLoaders(files, fileLayerLoader);
        fireInputChanged();
      }
    });
  }

  public void enteredFromLeft(final Map dataMap) {
    rescanCurrentDirectory();
    state.setCurrentPanel(KEY);
  }

  public void exitingToRight() throws Exception {
    blackboard.put(LoadFileDataSourceQueryChooser.FILE_CHOOSER_DIRECTORY_KEY,
      getCurrentDirectory().getAbsolutePath());
  }

  public String getID() {
    return getClass().getName();
  }

  public String getInstructions() {
    return "Select the files to load into the current project";
  }

  public String getNextID() {
    return state.getNextPanel(KEY);
  }

  public String getTitle() {
    return "Select Files";
  }

  public boolean isInputValid() {
    return state.hasSelectedFiles();
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
