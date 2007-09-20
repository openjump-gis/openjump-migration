package org.openjump.core.ui.plugin.file;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuListener;

import org.openjump.core.ui.enablecheck.BooleanPropertyMenuEnableListener;
import org.openjump.swing.listener.InvokeMethodPropertyChangeListener;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;

public class OpenRecentPlugIn extends AbstractPlugIn {
  private static final String KEY = OpenRecentPlugIn.class.getName();

  private static final String RECENT_FILES_KEY = KEY + ".FILES";

  private static final String RECENT_PROJECTS_KEY = KEY + ".PROJECTS";

  public static OpenRecentPlugIn get(final WorkbenchContext context) {
    Blackboard blackboard = context.getBlackboard();
    synchronized (KEY) {
      OpenRecentPlugIn plugin = (OpenRecentPlugIn)blackboard.get(KEY);
      if (plugin == null) {
        plugin = new OpenRecentPlugIn();
        try {
          plugin.initialize(new PlugInContext(context, null, null, null, null));
        } catch (Throwable e) {
          context.getErrorHandler().handleThrowable(e);
        }
      }
      return plugin;
    }
  }

  /** The listeners */
  private final Set<PropertyChangeListener> listeners = new LinkedHashSet<PropertyChangeListener>();

  /** The ordered set of recent files. */
  private Set<String> recentFiles = new LinkedHashSet<String>();

  /** The ordered set of recent projects. */
  private Set<String> recentProjects = new LinkedHashSet<String>();

  private String name = I18N.get(KEY);


  public void initialize(PlugInContext context) throws Exception {
    this.workbenchContext = context.getWorkbenchContext();
    workbenchContext.getBlackboard().put(KEY, this);
    recentFiles = getFileNames(RECENT_FILES_KEY);
    recentProjects = getFileNames(RECENT_PROJECTS_KEY);
    FeatureInstaller featureInstaller = context.getFeatureInstaller();
    recentMenu = FeatureInstaller.addMainMenu(featureInstaller, new String[] {
      MenuNames.FILE
    }, name, 0);
    InvokeMethodPropertyChangeListener listener = new InvokeMethodPropertyChangeListener(this, "updateFileAndProjectMenu", new Object[] {recentMenu});
    addPropertyChangeListener(listener);
    updateFileAndProjectMenu(recentMenu);
  }

  private OpenRecentPlugIn() {
  }

  public synchronized List<String> getRecentFiles() {
    return new ArrayList<String>(recentFiles);
  }

  public synchronized List<String> getRecentProjects() {
    return new ArrayList<String>(recentProjects);
  }

  private synchronized Set<String> getFileNames(String key) {
    Blackboard blackboard = PersistentBlackboardPlugIn.get(workbenchContext);
    Set<String> fileNames = (Set<String>)blackboard.get(key);
    if (fileNames == null) {
      fileNames = new LinkedHashSet<String>();
      blackboard.put(key, fileNames);
    }
    return fileNames;
  }

  public void addRecentFile(final File file) {
    addRecent(recentFiles, RECENT_FILES_KEY, file);
  }

  public void addRecentProject(final File file) {
    addRecent(recentProjects, RECENT_PROJECTS_KEY, file);
  }

  public void updateMenu() {
    updateFileAndProjectMenu(recentMenu);
  }

  private synchronized void addRecent(final Set<String> files,
    final String key, final File file) {
    try {

      String fileName = file.getCanonicalPath();
      files.remove(fileName);
      files.add(fileName);
      while (files.size() > 10) {
        files.remove(files.iterator().next());
      }
    } catch (IOException e) {
    }
    firePropertyChange(new PropertyChangeEvent(OpenRecentPlugIn.class, key,
      null, files));
  }

  private void addPropertyChangeListener(final PropertyChangeListener listener) {
    listeners.add(listener);
  }

  private void firePropertyChange(final PropertyChangeEvent event) {
    for (PropertyChangeListener listener : listeners) {
      listener.propertyChange(event);
    }
  }

  private WorkbenchContext workbenchContext;

  private JMenu recentMenu;

  public boolean hasRecentItems() {
    return !recentFiles.isEmpty() || !recentProjects.isEmpty();
  }

  public synchronized void updateFileAndProjectMenu(final JMenu recentMenu) {
    for (MenuListener listener : recentMenu.getMenuListeners()) {
      recentMenu.removeMenuListener(listener);
    }
    recentMenu.setEnabled(hasRecentItems());

    recentMenu.removeAll();
    List<String> files = getRecentFiles();
    Collections.reverse(files);
    for (String fileName : files) {
      File file = new File(fileName);
      OpenFilePlugin openFilePlugin = new OpenFilePlugin(workbenchContext, file);
      addRecentItem(recentMenu, file, openFilePlugin);
    }
    List<String> projects = getRecentProjects();
    if (!files.isEmpty() && !projects.isEmpty()) {
      recentMenu.addSeparator();
    }
    Collections.reverse(projects);
    for (String fileName : projects) {
      File file = new File(fileName);
      OpenProjectPlugIn openProjectPlugin = new OpenProjectPlugIn(
        workbenchContext, file);
      addRecentItem(recentMenu, file, openProjectPlugin);

    }
  }

  private void addRecentItem(final JMenu recentMenu, File file,
    AbstractPlugIn plugin) {
    JMenuItem menuItem = new JMenuItem(file.getName());

    String absolutePath = file.getAbsolutePath();
    ActionListener openFileListener = toActionListener(plugin,
      workbenchContext, new TaskMonitorManager());
    menuItem.addActionListener(openFileListener);
    recentMenu.addMenuListener(new BooleanPropertyMenuEnableListener(menuItem,
      file, "exists", absolutePath, "File does not exist: " + absolutePath));

    recentMenu.add(menuItem);
  }
}
