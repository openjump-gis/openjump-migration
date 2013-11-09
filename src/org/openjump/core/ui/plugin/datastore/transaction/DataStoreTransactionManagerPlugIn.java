package org.openjump.core.ui.plugin.datastore.transaction;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.WorkbenchContextReference;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxPlugIn;
import org.apache.log4j.Logger;
import org.openjump.core.model.TaskEvent;
import org.openjump.core.model.TaskListener;
import org.openjump.core.ui.plugin.datastore.WritableDataStoreDataSource;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.*;

/**
 * DataStoreTransactionManagerPlugIn is a ToolBox containing:
 * <ul>
 *     <li>a text area displaying uncommitted edits</li>
 *     <li>a commit button : to commit changes to the database</li>
 *     <li>an update button : to update changes from the database</li>
 *     <li>an inspect button : to inspect local evolutions not yet commited</li>
 * </ul>
 */
public class DataStoreTransactionManagerPlugIn extends ToolboxPlugIn implements WorkbenchContextReference {

    final Logger LOG = Logger.getLogger(DataStoreTransactionManagerPlugIn.class);

    private static final String KEY = DataStoreTransactionManagerPlugIn.class.getName();
    private static final String INSTANCE_KEY = KEY + " - INSTANCE";

    TransactionManagerPanel transactionManagerPanel;
    WorkbenchContext context;

    public DataStoreTransactionManagerPlugIn() {
    }


    public static final DataStoreTransactionManagerPlugIn instance(Blackboard blackboard) {
        if (blackboard.get(INSTANCE_KEY) == null) {
            blackboard.put(INSTANCE_KEY, new DataStoreTransactionManagerPlugIn());
        }
        return (DataStoreTransactionManagerPlugIn) blackboard.get(INSTANCE_KEY);
    }

    public void initialize(final PlugInContext context) throws Exception {
        context.getWorkbenchFrame().getToolBar().addPlugIn(
                IconLoader.icon("database_writable_add.png"), this, null, context.getWorkbenchContext());

        setWorkbenchContext(context.getWorkbenchContext());

        // Every time a new Task is added, a listener is added to the task
        // and check which one is activated/deactivated in order to update
        // TransactionManagerPanel accordingly.
        // Warning : when the listener is added to the TaskFrame, the
        // TransactionManagerPanel has not yet been created.
        context.getWorkbenchFrame().addTaskListener(new TaskListener() {
            public void taskAdded(TaskEvent taskEvent) {
                final Task task = taskEvent.getTask();
                LOG.info("Task added : " + task.getName());
                java.util.List<Layer> layers = task.getLayerManager().getLayers();
                for (Layer layer : layers) {
                    if (layer.getDataSourceQuery().getDataSource() instanceof WritableDataStoreDataSource) {
                        DataStoreTransactionManager.getTransactionManager().registerLayer(layer, task);
                    }
                }
                addListenerToTaskFrame(task);
            }
            public void taskLoaded(TaskEvent taskEvent) {
                final Task task = taskEvent.getTask();
                LOG.info("Task loaded : " + task.getName());
                java.util.List<Layer> layers = taskEvent.getTask().getLayerManager().getLayers();
                for (Layer layer : layers) {
                    if (layer.getDataSourceQuery().getDataSource() instanceof WritableDataStoreDataSource) {
                        DataStoreTransactionManager.getTransactionManager().registerLayer(layer, task);
                    }
                }
                addListenerToTaskFrame(task);
            }
        });
    }

    protected void initializeToolbox(final ToolboxDialog toolbox) {
        DataStoreTransactionManager txManager = DataStoreTransactionManager.getTransactionManager();
        transactionManagerPanel = new TransactionManagerPanel(txManager,
                toolbox.getContext().getErrorHandler(), toolbox.getContext());
        //transactionManagerPanel.init(toolbox.getContext());
        transactionManagerPanel.updateListener(toolbox.getContext().getTask());
        toolbox.getCenterPanel().add(transactionManagerPanel, BorderLayout.CENTER);
        toolbox.pack();
        toolbox.setInitialLocation(new GUIUtil.Location(20, true, 40, false));
    }

    // Add a listener to the task and update the TransactionManagerPanel when the task is activated
    private void addListenerToTaskFrame(final Task task) {
        getTaskFrame().addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                LOG.trace("Task frame activated " + e.getInternalFrame().getTitle());
                if (getTransactionManagerPanel() != null) {
                    getTransactionManagerPanel().updateListener(task);
                }
            }
        });
        if (getTransactionManagerPanel() != null) {
            getTransactionManagerPanel().updateListener(task);
        }
    }

    TaskFrame getTaskFrame() {
        return context.getWorkbench().getFrame().getActiveTaskFrame();
    }

    private TransactionManagerPanel getTransactionManagerPanel() {
        return transactionManagerPanel;
    }

    public boolean execute(PlugInContext context) throws Exception {
        boolean b = super.execute(context);
        if (transactionManagerPanel != null) transactionManagerPanel.updateTextArea(context.getTask());
        return b;
    }

    public void setWorkbenchContext(WorkbenchContext context) {
        this.context = context;
    }

}
