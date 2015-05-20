package org.openjump.core.ui.plugin.layer.pirolraster;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.ui.plugin.layer.pirolraster.panel.RasterColorEditorPanel;
import org.openjump.core.ui.plugin.layer.pirolraster.panel.RasterScaleStylePanel;
import org.openjump.core.ui.plugin.layer.pirolraster.panel.RasterTransparencyPanel;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.style.StylePanel;

/**
 * @version $Rev: 4221 $ Dic 23 2014 [Giuseppe Aruta] - Transformed in a
 *          MultiImputDialog. Add Added Raster Transparency, ScaleStyle and
 *          ColorEditor Panels
 */
@SuppressWarnings("deprecation")
public class ChangeRasterImagePropertiesPlugIn extends AbstractPlugIn {
    private final static String LAST_TAB_KEY = ChangeRasterImagePropertiesPlugIn.class
            .getName() + " - LAST TAB";

    public ChangeRasterImagePropertiesPlugIn() {
    }

    /*
     * Deactivated public void initialize(PlugInContext context) throws
     * Exception { WorkbenchContext workbenchContext =
     * context.getWorkbenchContext(); new FeatureInstaller(workbenchContext);
     * context.getFeatureInstaller().addMainMenuPlugin( this, new String[]
     * {MenuNames.RASTER}, getName(), false, getIcon(),
     * createEnableCheck(context.getWorkbenchContext())); }
     */
    public boolean execute(PlugInContext context) throws Exception {
        final RasterImageLayer rLayer = (RasterImageLayer) LayerTools
                .getSelectedLayerable(context, RasterImageLayer.class);
        final MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(),
                I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.ChangeRasterImagePropertiesPlugIn.Change-Raster-Image-Properties"),
                true);

        dialog.setSize(500, 400);
        dialog.setInset(0);
        dialog.setApplyVisible(true);

        final ArrayList<JPanel> stylePanels = new ArrayList();
        final RasterScaleStylePanel rasterScalepanel = new RasterScaleStylePanel(
                rLayer, context.getLayerViewPanel());
        final RasterTransparencyPanel rasstyle = new RasterTransparencyPanel(
                rLayer);
        final RasterColorEditorPanel rascolorpanel = new RasterColorEditorPanel(
                context, rLayer);

        if (rLayer.getNumBands() == 1) {
            stylePanels.add(rasstyle);
            stylePanels.add(rasterScalepanel);
            stylePanels.add(rascolorpanel);
        } else {
            stylePanels.add(rasstyle);
            stylePanels.add(rasterScalepanel);
            // stylePanels.add(rascolorpanel);
        }

        JTabbedPane tabbedPane = new JTabbedPane();

        for (Iterator i = stylePanels.iterator(); i.hasNext();) {
            final StylePanel stylePanel = (StylePanel) i.next();
            tabbedPane.add((Component) stylePanel, stylePanel.getTitle());
            dialog.addEnableChecks(stylePanel.getTitle(),
                    Arrays.asList(new EnableCheck[] { new EnableCheck() {
                        public String check(JComponent component) {
                            return stylePanel.validateInput();
                        }
                    } }));
        }

        dialog.addRow(tabbedPane);
        tabbedPane.setSelectedComponent(find(
                stylePanels,
                (String) context
                        .getWorkbenchContext()
                        .getWorkbench()
                        .getBlackboard()
                        .get(LAST_TAB_KEY,
                                ((StylePanel) stylePanels.iterator().next())
                                        .getTitle())));

        context.getWorkbenchContext()
                .getWorkbench()
                .getBlackboard()
                .put(LAST_TAB_KEY,
                        ((StylePanel) tabbedPane.getSelectedComponent())
                                .getTitle());

        dialog.addOKCancelApplyPanelActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (dialog.wasApplyPressed()) {
                    if (rLayer.getNumBands() == 1) {
                        rasterScalepanel.updateStyles();
                        rasstyle.updateStyles();
                        rascolorpanel.updateStyles();
                    } else {
                        rasterScalepanel.updateStyles();
                        rasstyle.updateStyles();
                        // rascolorpanel.updateStyles();
                    }
                }
            }
        });

        dialog.pack();
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);

        if (dialog.wasOKPressed()) {
            if (rLayer.getNumBands() == 1) {
                rasterScalepanel.updateStyles();
                rasstyle.updateStyles();
                rascolorpanel.updateStyles();
            } else {
                rasterScalepanel.updateStyles();
                rasstyle.updateStyles();
                // rascolorpanel.updateStyles();
            }
            return true;
        }
        return false;
    }

    private Component find(Collection stylePanels, String title) {
        for (Iterator i = stylePanels.iterator(); i.hasNext();) {
            StylePanel stylePanel = (StylePanel) i.next();

            if (stylePanel.getTitle().equals(title)) {
                return (Component) stylePanel;
            }
        }

        Assert.shouldNeverReachHere();

        return null;
    }

    public String getName() {
        return I18N
                .get("org.openjump.core.ui.plugin.layer.pirolraster.ChangeRasterImagePropertiesPlugIn.Change-Raster-Image-Properties");
    }

    public ImageIcon getIcon() {
        return GUIUtil.toSmallIcon(IconLoader.icon("Palette.png"));
    }

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {

        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();
        multiEnableCheck.add(checkFactory
                .createExactlyNLayerablesMustBeSelectedCheck(1,
                        RasterImageLayer.class));
        return multiEnableCheck;
    }

}
