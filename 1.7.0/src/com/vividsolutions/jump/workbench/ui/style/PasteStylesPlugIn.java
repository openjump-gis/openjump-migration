package com.vividsolutions.jump.workbench.ui.style;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageStyle;
import com.vividsolutions.jump.workbench.imagery.ReferencedImagesLayer;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.ImageIcon;

/**
 * Pastes the styles from the internal style paste buffer to a layer
 * @author Martin Davis
 * @version 1.0
 */

public class PasteStylesPlugIn extends AbstractPlugIn {

  public static MultiEnableCheck createEnableCheck(
      final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    return new MultiEnableCheck().add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
        .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1));
  }


  public PasteStylesPlugIn() {
  }

  public String getName() {
  	return I18N.get("ui.style.PasteStylesPlugIn.paste-styles");
  }
  
  public ImageIcon getIcon() {
    return GUIUtil.toSmallIcon(IconLoader.icon("Palette_out.gif"));
  }

  public boolean execute(PlugInContext context) throws Exception
  {
    if (CopyStylesPlugIn.stylesBuffer == null)
      return false;
    Layer[] selectedLayers = context.getSelectedLayers();
    for (int i = 0; i < selectedLayers.length; i++) {
      pasteStyles(selectedLayers[i]);
    }
    return true;
  }

  private void pasteStyles(Layer layer) throws Exception
  {
    layer.setScaleDependentRenderingEnabled(CopyStylesPlugIn.isScaleDependentRenderingEnabled);
    layer.setMaxScale(CopyStylesPlugIn.maxScale);
    layer.setMinScale(CopyStylesPlugIn.minScale);
    if (CopyStylesPlugIn.stylesBuffer.size() > 0 &&
          CopyStylesPlugIn.stylesBuffer.iterator().next() instanceof ReferencedImageStyle) {
        // don't try to copy style from image layer
    } else {
        validateStyleForLayer(layer); //throws exception if bad
        layer.setStyles(CopyStylesPlugIn.stylesBuffer);
    }
  }
  
  private void validateStyleForLayer(Layer layer) throws IllegalArgumentException {
        if (layer instanceof ReferencedImagesLayer) return;
		String attribName = "";
		try {
			FeatureSchema featureSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
			ArrayList<String> attribList = new ArrayList<String>();
			for (Iterator i = CopyStylesPlugIn.stylesBuffer.iterator(); i.hasNext();) {
				Style style = (Style) i.next();
				if (style instanceof LabelStyle) {
					LabelStyle labelStyle = (LabelStyle) style;
					if (labelStyle.isEnabled()) {
						attribName =labelStyle.getAttribute();
						if (!attribName.equals("")) attribList.add(attribName);
						attribName =labelStyle.getAngleAttribute();
						if (!attribName.equals("")) attribList.add(attribName);
						attribName =labelStyle.getHeightAttribute();
						if (!attribName.equals("")) attribList.add(attribName);
					}
				} else if (style instanceof ColorThemingStyle) {
					ColorThemingStyle ctStyle = (ColorThemingStyle) style;
					if (ctStyle.isEnabled()) {
						attribName =ctStyle.getAttributeName();
						if (attribName != "") attribList.add(attribName);
					}
				}
			}
			for (Iterator i = attribList.iterator(); i.hasNext();) {
				attribName = (String) i.next();
				if (attribName.equals(LabelStyle.FID_COLUMN)) continue;
				//check for attribute in Layer's FeatureSchema and throw exception now if not found.
				featureSchema.getAttributeIndex(attribName);
			}
		} catch (IllegalArgumentException ex){
			throw new IllegalArgumentException("Attribute "+ attribName+
					" not found on Layer: "+ layer.getName());
		}
	  }

}