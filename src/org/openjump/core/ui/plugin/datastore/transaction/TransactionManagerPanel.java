package org.openjump.core.ui.plugin.datastore.transaction;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.plugin.WorkbenchContextReference;
import org.apache.log4j.Logger;
import org.openjump.core.ui.plugin.datastore.WritableDataStoreDataSource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * Panel displaying current uncommitted edits and including the commit button.
 */
public class TransactionManagerPanel extends JPanel  implements WorkbenchContextReference {

    private static final String KEY = TransactionManagerPanel.class.getName();

    final Logger LOG = Logger.getLogger(TransactionManagerPanel.class);

    final DataStoreTransactionManager transactionManager;
    final ErrorHandler errorHandler;
    final JTextArea textArea;
    LayerListener layerListener;
    WorkbenchContext context;

    public TransactionManagerPanel(DataStoreTransactionManager transactionManager,
                                   ErrorHandler errorHandler, WorkbenchContext context) {
        this.transactionManager = transactionManager;
        this.errorHandler = errorHandler;
        textArea = new JTextArea(12,32);
        init(context);
    }

    private void init(WorkbenchContext context) {
        this.context = context;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        textArea.setFont(textArea.getFont().deriveFont(11f));
        this.add(new JScrollPane(textArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2,4,2,4);
        c.gridx = 0;
        c.gridy = 0;

        JLabel experimental = new JLabel("<html><font size='6' color='red'>Experimental</font><html>");
        c.gridwidth = 2;
        panel.add(experimental, c);

        JButton inspectButton = new JButton(I18N.get(KEY + ".inspect"));
        inspectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                transactionManager.inspect(getTaskFrame());
            }
        });
        c.gridx = 1;
        c.gridy += 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(inspectButton, c);

        JButton updateButton = new JButton(I18N.get(KEY + ".update"));
        updateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                transactionManager.update(getTaskFrame());
            }
        });
        c.gridx = 0;
        c.gridy += 1;
        panel.add(updateButton, c);

        JButton commitButton = new JButton(I18N.get(KEY + ".commit"));
        commitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    transactionManager.commit();
                    updateTextArea(JUMPWorkbench.getInstance().getContext().getTask());
                } catch(Exception ex) {
                    errorHandler.handleThrowable(ex);
                }
            }
        });
        c.gridx = 1;
        //c.gridy = 1;
        panel.add(commitButton, c);

        this.add(panel);

        updateListener(context.getTask());
    }

    /**
     * Remove the layerListener displaying all feature events in the text area
     * create a new one and add it to the current TaskFrame.
     * This method ensure all current TaskFrame listen to feature events, even
     * if a new Task has been created since the plugin initialization or execution.
     */
    public void updateListener(final Task task) {
        if (task == null) return;
        for (JInternalFrame iframe : JUMPWorkbench.getInstance().getFrame().getInternalFrames()) {
            if (iframe instanceof TaskFrame) {
                ((TaskFrame)iframe).getTask().getLayerManager().removeLayerListener(layerListener);
            }
        }
        layerListener = new LayerAdapter() {
            public void featuresChanged(FeatureEvent e) {
                Layer layer = e.getLayer();
                Collection layers = transactionManager.getLayers();
                if (layers.contains(layer)) {
                    DataSource dataSource = layer.getDataSourceQuery().getDataSource();
                    if (dataSource instanceof WritableDataStoreDataSource) {
                        updateTextArea(task);
                    }
                    else {
                        LOG.error(I18N.get(KEY + ".layer-with-irrelevant-datastore-datasource"));
                    }
                }
            }
            public void layerChanged(LayerEvent e) {
                if (e.getType() == LayerEventType.REMOVED) {
                    updateTextArea(task);
                }
            }
        };
        for (JInternalFrame iframe : JUMPWorkbench.getInstance().getFrame().getInternalFrames()) {
            if (iframe instanceof TaskFrame) {
                ((TaskFrame)iframe).getTask().getLayerManager().addLayerListener(layerListener);
            }
        }
        updateTextArea(task);
    }

    void updateTextArea(Task task) {
        textArea.setText("");
        for (Layer layer : transactionManager.getLayers()) {
            // @TODO is it safe to use != ?
            if (transactionManager.getTask(layer) != task) continue;
            if (layer.getDataSourceQuery().getDataSource() instanceof WritableDataStoreDataSource) {
                int c = 0, m = 0, s = 0;
                for (Evolution evo : ((WritableDataStoreDataSource)layer.getDataSourceQuery().getDataSource()).getUncommittedEvolutions()) {
                    if (evo.getType() == Evolution.Type.CREATION) c++;
                    if (evo.getType() == Evolution.Type.SUPPRESSION) s++;
                    if (evo.getType() == Evolution.Type.MODIFICATION) m++;
                }
                if (c+m+s>0) textArea.append(layer.getName()+":\n");
                if (c>0) textArea.append(I18N.getMessage(KEY + ".creations", c) + "\n");
                if (s>0) textArea.append(I18N.getMessage(KEY + ".suppressions", s) + "\n");
                if (m>0) textArea.append(I18N.getMessage(KEY + ".modifications", m) + "\n");
            }
        }
    }

    TaskFrame getTaskFrame() {
        return context.getWorkbench().getFrame().getActiveTaskFrame();
    }

    public void setWorkbenchContext(WorkbenchContext context) {
        this.context = context;
    }

}