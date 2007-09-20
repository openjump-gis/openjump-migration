package org.openjump.core.ui.plugin.file;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.Icon;

import org.openjump.core.ui.io.file.DataSourceFileLayerLoader;
import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.core.ui.plugin.file.open.OpenFileWizardState;
import org.openjump.core.ui.plugin.file.open.SelectFileLoaderPanel;
import org.openjump.core.ui.plugin.file.open.SelectFileOptionsPanel;
import org.openjump.core.ui.plugin.file.open.SelectFilesPanel;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooserManager;
import com.vividsolutions.jump.workbench.datasource.FileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.datasource.LoadDatasetPlugIn;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.WorkbenchToolBar;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class OpenFilePlugin extends ThreadedBasePlugIn {

  private WorkbenchContext workbenchContext;

  private Registry registry;

  private OpenFileWizardState state;

  private File file;

  private OpenRecentPlugIn recentPlugin;

  public OpenFilePlugin() {
  }

  public OpenFilePlugin(final WorkbenchContext workbenchContext, final File file) {
    this.file = file;
    initialize(workbenchContext);
  }

  private void initialize(WorkbenchContext workbenchContext) {
    this.workbenchContext = workbenchContext;
    registry = workbenchContext.getRegistry();
    recentPlugin = OpenRecentPlugIn.get(workbenchContext);
  }

  public void initialize(PlugInContext context) throws Exception {
    initialize(context.getWorkbenchContext());
    FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);

    JUMPWorkbench workbench = workbenchContext.getWorkbench();
    WorkbenchFrame frame = workbench.getFrame();

    EnableCheck enableCheck = createEnableCheck(workbenchContext);
    Icon icon = getIcon();
    String name = getName() + "...{pos:0}";
    // Add File Menu
    featureInstaller.addMainMenuItemWithJava14Fix(this, new String[] {
      MenuNames.FILE
    }, name, false, icon, enableCheck);

    // Add toolbar Icon
    WorkbenchToolBar toolBar = frame.getToolBar();
    toolBar.addPlugIn(1, this, icon, enableCheck, workbenchContext);

    // Add layer popup menu
    featureInstaller.addPopupMenuItem(frame.getCategoryPopupMenu(), this, name,
      false, icon, enableCheck);
  }

  public static EnableCheck createEnableCheck(
    final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

    return checkFactory.createWindowWithLayerManagerMustBeActiveCheck();
  }

  public static String getMenuName() {
    return "Open File...";
  }

  public boolean execute(PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    LayerManager layerManager = context.getLayerManager();
    if (layerManager == null) {
      // TODO what if there is no active project?
    }

    WizardDialog dialog = new WizardDialog(context.getWorkbenchFrame(),
      "Open Files", context.getErrorHandler());
    OpenFileWizardState state = new OpenFileWizardState(
      context.getErrorHandler());
    List loaders = registry.getEntries(FileLayerLoader.KEY);
    for (Object loader : loaders) {
      FileLayerLoader fileLayerLoader = (FileLayerLoader)loader;
      state.addFileLoader(fileLayerLoader);
    }
    dialog.setData(OpenFileWizardState.KEY, state);
    SelectFilesPanel filesPanel = new SelectFilesPanel(workbenchContext, state);
    SelectFileLoaderPanel loaderPanel = new SelectFileLoaderPanel(state);
    SelectFileOptionsPanel optionsPanel = new SelectFileOptionsPanel(
      workbenchContext, state);
    WizardPanel[] wizardPanels = null;
    if (file != null) {
      state.setupFileLoaders(new File[] {
        file
      }, null);
      String nextPanel = state.getNextPanel(SelectFilesPanel.KEY);
      if (nextPanel == SelectFileLoaderPanel.KEY) {
        wizardPanels = new WizardPanel[] {
          loaderPanel, optionsPanel
        };
      } else if (nextPanel == SelectFileOptionsPanel.KEY) {
        wizardPanels = new WizardPanel[] {
          optionsPanel
        };
      } else {
        this.state = state;
        return true;
      }
    } else {
      wizardPanels = new WizardPanel[] {
        filesPanel, loaderPanel, optionsPanel
      };
    }
    dialog.init(wizardPanels);

    dialog.setSize(700, 580);
    dialog.setVisible(true);
    if (dialog.wasFinishPressed()) {
      this.state = state;
      return true;
    }

    return false;

  }

  /**
   * Perform the actual loading of the files in a separate thread.
   */
  public void run(final TaskMonitor monitor, final PlugInContext context)
    throws Exception {
    Set<File> openedFiles = new LinkedHashSet<File>();
    try {
      monitor.allowCancellationRequests();
      for (Entry<URI, FileLayerLoader> entry : state.getFileLoaders()
        .entrySet()) {
        URI uri = entry.getKey();
        FileLayerLoader loader = entry.getValue();
        Map<String, Object> options = state.getOptions(uri);
        try {
          if (loader.open(monitor, uri, options)) {
            if (uri.getScheme().equals("zip")) {
              openedFiles.add(org.openjump.util.UriUtil.getZipFile(uri));
            } else {
              openedFiles.add(new File(uri));
            }
          }
        } catch (Exception e) {
          monitor.report(e);
        }
      }
    } finally {
      for (File file : openedFiles) {
        recentPlugin.addRecentFile(file);
      }
      state = null;
    }
  }

  public Icon getIcon() {
    return IconLoader.icon("Open.gif");
  }
}
