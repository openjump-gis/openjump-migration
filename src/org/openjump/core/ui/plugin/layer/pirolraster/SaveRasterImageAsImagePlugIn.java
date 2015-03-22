/*
 * Created on 29.06.2005 for Pirol
 *
 * SVN header information:
 * $Author: LBST-PF-3\orahn $
 * $Rev: 2509 $
 * $Date: 2006-10-06 10:01:50 +0000 (Fr, 06 Okt 2006) $
 * $Id: SaveRasterImageAsImagePlugIn.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.ui.plugin.layer.pirolraster;

import java.io.File;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageIOUtils;
import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.SaveFileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

/**
 * This PlugIn saves a RasterImages to disk with its geographical position. This
 * class is based on Stefan Ostermanns SaveInterpolationAsImagePlugIn.
 * 
 * @author Ole Rahn, Stefan Ostermann, <br>
 * <br>
 *         FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck, <br>
 *         Project: PIROL (2005), <br>
 *         Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2509 $ [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 * @version $Rev: 4345 $ [Giuseppe Aruta] - 22.Mar.2015 - rewrite class using
 *          new RasterImage I/O components. This version allows to export no
 *          data cell value to the output tif
 */
public class SaveRasterImageAsImagePlugIn extends AbstractPlugIn {

    private static final String FILE_CHOOSER_DIRECTORY_KEY = SaveFileDataSourceQueryChooser.class
            .getName() + " - FILE CHOOSER DIRECTORY";

    private static String ERROR = I18N
            .get("org.openjump.core.ui.plugin.mousemenu.SaveDatasetsPlugIn.Error-See-Output-Window");
    private static String PLUGINNAME = I18N
            .get("org.openjump.core.ui.plugin.layer.pirolraster.SaveRasterImageAsImagePlugIn.Save-Raster-Image-As-Image");
    private static String SAVED = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.saved");
    private static HashMap extensions;

    public static final String TIF_EXTENSION = "TIF";
    private static File file;

    public SaveRasterImageAsImagePlugIn() {
        this.extensions = new HashMap();

        this.extensions.put("TIF", "TIF");
    }

    @Override
    public String getName() {
        return PLUGINNAME;
    }

    public static final ImageIcon ICON = IconLoader.icon("disk_dots.png");

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);

        saveSingleRaster(context);

        return true;

    }

    public static void saveSingleRaster(PlugInContext context) {
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools
                .getSelectedLayerable(context, RasterImageLayer.class);
        Envelope env = rLayer.getWholeImageEnvelope();
        int bands = rLayer.getNumBands();
        JFileChooser fileChooser = GUIUtil
                .createJFileChooserWithOverwritePrompting();
        fileChooser.setDialogTitle(PLUGINNAME);

        if (PersistentBlackboardPlugIn.get(context.getWorkbenchContext()).get(
                FILE_CHOOSER_DIRECTORY_KEY) != null) {
            fileChooser.setCurrentDirectory(new File(
                    (String) PersistentBlackboardPlugIn.get(
                            context.getWorkbenchContext()).get(
                            FILE_CHOOSER_DIRECTORY_KEY)));
        }

        fileChooser.setMultiSelectionEnabled(false);

        fileChooser.setFileFilter(GUIUtil.createFileFilter("TIF",
                new String[] { "tif" }));

        int option;

        option = fileChooser.showSaveDialog(context.getWorkbenchFrame());

        if (option == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();

            file = FileUtil.addExtensionIfNone(file, "tif");
            String extension = FileUtil.getExtension(file);

            int band;

            band = 0;

            try {
                String trueExtension = (String) extensions.get(extension
                        .toUpperCase());

                RasterImageIOUtils.saveTIF(file, rLayer, env);

            } catch (Exception e) {
                context.getWorkbenchFrame().warnUser(ERROR);
                context.getWorkbenchFrame().getOutputFrame()
                        .createNewDocument();
                context.getWorkbenchFrame()
                        .getOutputFrame()
                        .addText(
                                "SaveImageToRasterPlugIn Exception:"
                                        + new Object[] { e.toString() });
                return;
            }

            rLayer.setImageFileName(file.getPath());
            rLayer.setNeedToKeepImage(false);
            context.getWorkbenchFrame().setStatusMessage(SAVED);

        }

        return;

    }

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();
        multiEnableCheck.add(checkFactory
                .createAtLeastNLayerablesMustBeSelectedCheck(1,
                        RasterImageLayer.class));

        return multiEnableCheck;
    }

}