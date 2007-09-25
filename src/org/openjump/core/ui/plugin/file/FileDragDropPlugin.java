package org.openjump.core.ui.plugin.file;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.iharder.dnd.FileDrop;

import org.openjump.core.ui.plugin.AbstractPlugIn;

import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

public class FileDragDropPlugin extends AbstractPlugIn {
  public static final Set<String> PROJECT_EXTENSIONS = new HashSet<String>(
    Arrays.asList(new String[] {
      "jmp", "jcs"
    }));

  /**
   * Initialise plug-in.
   * 
   * @param context The plug-in context.
   * @exception Exception If there was an error initialising the plug-in.
   */
  public void initialize(final PlugInContext context) throws Exception {
    super.initialize(context);
    JUMPWorkbench workbench = workbenchContext.getWorkbench();
    WorkbenchFrame frame = workbench.getFrame();

    new FileDrop(frame, new FileDrop.Listener() {
      public void filesDropped(File[] files) {
        List<File> projectFiles = new ArrayList<File>();
        List<File> dataFiles = new ArrayList<File>();
        for (File file : files) {
          String extension = GUIUtil.getExtension(file);
          if (PROJECT_EXTENSIONS.contains(extension)) {
            projectFiles.add(file);
          } else {
            dataFiles.add(file);
          }
        }

        if (!dataFiles.isEmpty()) {
          OpenFilePlugIn filePlugin = new OpenFilePlugIn(workbenchContext,
            dataFiles.toArray(new File[0]));
          filePlugin.actionPerformed(new ActionEvent(this, 0, ""));
        }

        if (!projectFiles.isEmpty()) {
          OpenProjectPlugIn projectPlugin = new OpenProjectPlugIn(
            workbenchContext, projectFiles.toArray(new File[0]));
          projectPlugin.actionPerformed(new ActionEvent(this, 0, ""));
        }
      }
    });

  }
}
