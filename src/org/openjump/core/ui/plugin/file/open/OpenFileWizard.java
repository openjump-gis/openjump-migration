package org.openjump.core.ui.plugin.file.open;

import java.io.File;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openjump.core.ui.images.IconLoader;
import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.core.ui.plugin.file.OpenRecentPlugIn;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.ErrorHandlerV2;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;

public class OpenFileWizard extends AbstractWizardGroup {
  /** The key for the wizard. */
  public static final String KEY = OpenFileWizard.class.getName();

  /** The current state of the wizard. */
  private OpenFileWizardState state;

  /** The workbench context. */
  private WorkbenchContext workbenchContext;

  private File[] files;

  private Class loaderFilter = null;

  private ChooseProjectPanel chooseProjectPanel;

  private SelectFilesPanel selectFilesPanel;

  private SelectFileLoaderPanel selectFileLoaderPanel;

  private SelectFileOptionsPanel selectFileOptionsPanel;
  
  private Layerable layer;

  public Layerable getLayer() {
    return layer;
  }

  public void setLayer(Layerable layer) {
    this.layer = layer;
  }

  /**
   * Construct a new OpenFileWizard.
   * 
   * @param workbenchContext
   *          The workbench context.
   */
  public OpenFileWizard(final WorkbenchContext workbenchContext) {
    super(I18N.get(KEY), IconLoader.icon("folder_page.png"),
        SelectFilesPanel.KEY);
    initPanels(workbenchContext);
  }

  public OpenFileWizard(final WorkbenchContext workbenchContext,
      final File[] files) {
    this.files = files;
  }

  public OpenFileWizard(final WorkbenchContext workbenchContext,
      final Class loaderFilter) {
    this.loaderFilter = loaderFilter;
    initPanels(workbenchContext);
  }

  public void initialize(final WorkbenchContext workbenchContext,
      WizardDialog dialog) {
    this.workbenchContext = workbenchContext;
    initPanels(workbenchContext);
    state = new OpenFileWizardState(workbenchContext.getErrorHandler());
    Registry registry = workbenchContext.getRegistry();
    List<FileLayerLoader> loaders = registry.getEntries(FileLayerLoader.KEY);
    for (FileLayerLoader fileLayerLoader : loaders) {
      if (loaderFilter != null && !loaderFilter.isInstance(fileLayerLoader))
        continue;
      state.addFileLoader(fileLayerLoader);
    }
    if (selectFilesPanel != null) {
      selectFilesPanel.setState(state);
      selectFilesPanel.setDialog(dialog);
    }
    selectFileLoaderPanel.setState(state);
    selectFileOptionsPanel.setState(state);
    if (files != null) {
      state.setupFileLoaders(files, null);
    }
  }

  protected void initPanels(final WorkbenchContext workbenchContext) {
    if (selectFileLoaderPanel == null) {
      if (files == null) {
        chooseProjectPanel = new ChooseProjectPanel(workbenchContext,
            SelectFilesPanel.KEY);
        addPanel(chooseProjectPanel);
        selectFilesPanel = new SelectFilesPanel(workbenchContext, loaderFilter);
        addPanel(selectFilesPanel);
      } else {
        chooseProjectPanel = new ChooseProjectPanel(workbenchContext,
            SelectFileLoaderPanel.KEY);
        addPanel(chooseProjectPanel);
      }
      selectFileLoaderPanel = new SelectFileLoaderPanel();
      addPanel(selectFileLoaderPanel);
      selectFileOptionsPanel = new SelectFileOptionsPanel(workbenchContext);
      addPanel(selectFileOptionsPanel);
    }
  }

  public String getFirstId() {
    String firstId;
    if (files != null) {
      firstId = state.getNextPanel(SelectFilesPanel.KEY);
    } else {
      firstId = SelectFilesPanel.KEY;
    }
    if (!chooseProjectPanel.hasActiveTaskFrame()
        && chooseProjectPanel.hasTaskFrames()) {
      chooseProjectPanel.setNextID(firstId);
      return chooseProjectPanel.getID();
    } else {
      return firstId;
    }
  }

  /**
   * Load the files selected in the wizard.
   * 
   * @param monitor
   *          The task monitor.
   */
  public void run(WizardDialog dialog, TaskMonitor monitor) throws Exception {
    chooseProjectPanel.activateSelectedProject();
    Set<File> openedFiles = new LinkedHashSet<File>();
    try {
      monitor.allowCancellationRequests();
      Map<URI, FileLayerLoader> fileLoaders = state.getFileLoaders();
      for (Entry<URI, FileLayerLoader> entry : fileLoaders.entrySet()) {
        URI uri = entry.getKey();
        FileLayerLoader loader = entry.getValue();
        Map<String, Object> options = state.getOptions(uri);
        if (layer!=null)
          options.put("LAYER", layer);
        try {
          if (loader.open(monitor, uri, options)) {
            if (uri.getScheme().equals("zip")) {
              openedFiles.add(org.openjump.util.UriUtil.getZipFile(uri));
            } else {
              openedFiles.add(new File(uri));
            }
          }
        } catch (final Exception e) {
          ((ErrorHandlerV2) workbenchContext.getErrorHandler())
              .handleThrowable(e, dialog);
        }
      }
    } finally {
      state = null;
      OpenRecentPlugIn recentPlugin = OpenRecentPlugIn.get(workbenchContext);
      for (File file : openedFiles) {
        recentPlugin.addRecentFile(file);
      }
    }
  }

}
