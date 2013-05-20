/*
 * Created on 16.03.2005 for PIROL
 *
 * CVS header information:
 *  $RCSfile: SetCategoryVisibilityPlugIn.java,v $
 *  $Revision: 1.6 $
 *  $Date: 2005/11/22 16:44:42 $
 *  $Source: D:/CVS/cvsrepo/pirolPlugIns/plugIns/CategoryTools/SetCategoryVisibilityPlugIn.java,v $
 */
package org.openjump.core.ui.plugin.mousemenu.category;

import java.awt.CheckboxMenuItem;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;

import org.apache.log4j.Logger;
import org.openjump.core.apitools.PlugInContextTools;

import de.fho.jump.pirol.plugins.EditAttributeByFormula.EditAttributeByFormulaPlugIn;
import de.fho.jump.pirol.utilities.debugOutput.DebugUserIds;
import de.fho.jump.pirol.utilities.debugOutput.PersonalLogger;
import de.fho.jump.pirol.utilities.plugIns.StandardPirolPlugIn;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelListener;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * @author Ole Rahn
 * 
 * FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck
 * Project PIROL 2005
 * Daten- und Wissensmanagement
 * 
 */
public class SetCategoryVisibilityPlugIn extends AbstractPlugIn implements LayerNamePanelListener {
    
    protected Map category2Visibility = new HashMap();
    protected Map layer2Visibility = new HashMap();
    protected PlugInContext context = null;
    
    protected JCheckBoxMenuItem menuItem = null;
    
    protected static SetCategoryVisibilityPlugIn instance= null;
    
    private static final Logger LOG = Logger.getLogger(SetCategoryVisibilityPlugIn.class);
    
    /**
     * Constructor needed to load PlugIn from classes, should NOT be used by any other
     * code --> use getInstance() method instead!!
     */
    private SetCategoryVisibilityPlugIn(){
        SetCategoryVisibilityPlugIn.instance = this;
    }
    
    public static SetCategoryVisibilityPlugIn getInstance(PlugInContext context){
        if (SetCategoryVisibilityPlugIn.instance == null){
            SetCategoryVisibilityPlugIn.instance = new SetCategoryVisibilityPlugIn();
            SetCategoryVisibilityPlugIn.instance.context = context;
        }
        
        return SetCategoryVisibilityPlugIn.instance;
    }

    public String getName(){
    	return 	I18N.get("org.openjump.core.ui.plugin.mousemenu.category.SetCategoryVisibilityPlugIn.Set-Category-Visibility");
    }
    
    public void initialize(PlugInContext context) throws Exception {
        /* add to pirol menu */
        //super.initialize(context);
        
        /* add to right-click menu */
        this.context = context;
        
        JPopupMenu layerNamePopupMenu = context.getWorkbenchContext().getWorkbench().getFrame().getCategoryPopupMenu();
        FeatureInstaller featInst = context.getFeatureInstaller();
        
        this.menuItem = (JCheckBoxMenuItem) featInst.addPopupMenuPlugin(layerNamePopupMenu,
                this, this.getName() + "...", true,
                GUIUtil.toSmallIcon((ImageIcon) this.getIcon()),
                SetCategoryVisibilityPlugIn.createEnableCheck(context.getWorkbenchContext()));
        
//        MenuElement[] elements = layerNamePopupMenu.getSubElements();
//        
//        for (int i=0; i<elements.length; i++){
//            if ( JCheckBoxMenuItem.class.isInstance(elements[i]) ){
//                if ( ((JCheckBoxMenuItem)elements[i]).getText().startsWith(this.getName()) ){
//                    ((JCheckBoxMenuItem)elements[i]).setSelected(true);
//                    this.menuItem = (JCheckBoxMenuItem)elements[i];
//                }
//            }
//        }
        
//        context.getLayerNamePanel().addListener(this);
    }

    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();
        
        multiEnableCheck.add( checkFactory.createAtLeastNLayersMustExistCheck(1) );
        //multiEnableCheck.add( checkFactory.createAtLeastNCategoriesMustBeSelectedCheck(1) );
        multiEnableCheck.add( checkFactory.createExactlyNCategoriesMustBeSelectedCheck(1) );
        
        // simple hook to switch menuitem states
        multiEnableCheck.add(new EnableCheck() {
          public String check(JComponent component) {
            SetCategoryVisibilityPlugIn.getInstance(workbenchContext.createPlugInContext()).layerSelectionChanged();
            return null;
          }
        });
        
        return multiEnableCheck;
	}
    
    public Icon getIcon() {
        return new ImageIcon(getClass().getResource("eye.png"));
    }
    
    public boolean execute(PlugInContext context) throws Exception {
        
//        try {
//            context.getLayerNamePanel().addListener(this);
//        } catch (RuntimeException e) {
//            e.printStackTrace();
//        }
        
        Collection selCats = context.getLayerNamePanel().getSelectedCategories();
        
        Iterator iter = selCats.iterator();
        Category cat;
        Boolean visible;

        while(iter.hasNext()){
            cat = (Category)iter.next();
            
            if (!this.category2Visibility.containsKey(cat)){
                this.category2Visibility.put(cat, new Boolean(true));
            }
            
            visible = (Boolean)this.category2Visibility.get(cat);
            this.category2Visibility.remove(cat);
            this.category2Visibility.put(cat, new Boolean(!visible.booleanValue()) );
            
            this.setLayerVisibility( cat.getLayerables(), !visible.booleanValue() );
        }
        
        return true;
    }
    
    protected void setLayerVisibility(List layers, boolean visible){
        Iterator iter = layers.iterator();
        
        Layerable layer;
        
        while(iter.hasNext()){
            layer = (Layerable)iter.next();
            
            if (!visible && !this.layer2Visibility.containsKey(layer)){
                this.layer2Visibility.put(layer, new Boolean(layer.isVisible()));
            }
            
            if (layer.isVisible() != visible){
                if (!visible){
                    layer.setVisible(visible);
                } else {
                    if (this.layer2Visibility.containsKey(layer)){
                        layer.setVisible(((Boolean)this.layer2Visibility.get(layer)).booleanValue());
                        this.layer2Visibility.remove(layer);
                    } else {
                        layer.setVisible(visible);
                    }
                }
            }
            
        }
    }
    
    protected void checkAndFixInvisibility(){
        if (this.context == null){
            LOG.warn("SetCategoryVisibilityPlugIn: context == null!");
            return;
        }
        
        PlugInContext context = PlugInContextTools.getContext(this.context);
        List cathegories = context.getLayerManager().getCategories();
        
        Iterator iter = cathegories.iterator();
        Category cat;
        
        while (iter.hasNext()){
            cat = (Category)iter.next();
            if (this.category2Visibility.containsKey(cat) && !((Boolean)this.category2Visibility.get(cat)).booleanValue()){
                this.setLayerVisibility( cat.getLayerables(), false );
            }
        }
    }
    
    public boolean IsCategoryVisible(Category cat){
        if (this.category2Visibility.containsKey(cat))
            return ((Boolean)this.category2Visibility.get(cat)).booleanValue();

        // by default, there is no invisibilty for categories
        return true;
    }
    
    public void setCategoryVisibility(Category cat, boolean visible){
        this.category2Visibility.put(cat, new Boolean(visible));
        this.checkAndFixInvisibility();
    }

    public void layerSelectionChanged() {
        PlugInContext context = PlugInContextTools.getContext(this.context);
        List categories = context.getLayerManager().getCategories();
        
        Iterator iter = categories.iterator();
        Category cat;
        
        while (iter.hasNext()){
            cat = (Category)iter.next();
            if (!this.category2Visibility.containsKey(cat)){
                this.category2Visibility.put(cat, new Boolean(true));
            }
        }
        
        this.checkAndFixInvisibility();
        
        Collection selCats = context.getLayerNamePanel().getSelectedCategories();
        
        if (selCats.isEmpty()) return;
        
        cat = (Category)selCats.iterator().next();
        
        this.menuItem.setSelected( ((Boolean)this.category2Visibility.get(cat)).booleanValue() );

    }

}
