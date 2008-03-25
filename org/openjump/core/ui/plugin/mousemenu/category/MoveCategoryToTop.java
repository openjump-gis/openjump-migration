/*
 * Created on 03.05.2005 for PIROL
 *
 * CVS header information:
 *  $RCSfile: MoveCategoryToTop.java,v $
 *  $Revision: 1.2 $
 *  $Date: 2005/09/13 08:45:58 $
 *  $Source: D:/CVS/cvsrepo/pirolPlugIns/plugIns/CategoryTools/MoveCategoryToTop.java,v $
 */
package org.openjump.core.ui.plugin.mousemenu.category;

import java.util.Collection;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * 
 *
 * @author Ole Rahn
 * @author FH Osnabr�ck - University of Applied Sciences Osnabr�ck,
 * Project: PIROL (2005),
 * Subproject: Daten- und Wissensmanagement
 * 
 */
public class MoveCategoryToTop extends AbstractPlugIn {

    public boolean execute(PlugInContext context) throws Exception {
        CategoryMover cm = new CategoryMover(context);
        
        Collection cats = context.getLayerNamePanel().getSelectedCategories();
        
        if (cats.size() > 1 || cats.size() <= 0){
        	context.getWorkbenchFrame().warnUser("Only a single category can be moved!");
            return false;
        }
        
        Object[] catsArray = cats.toArray();
        
        cm.moveCategoryToTop((Category)catsArray[0]);
        
        return true;
    }

    public Icon getIcon() {
        return new ImageIcon(getClass().getResource("bullet_arrow_top.png"));
    }
    
    public void initialize(PlugInContext context) throws Exception {
        
        JPopupMenu layerNamePopupMenu = context.getWorkbenchContext().getWorkbench().getFrame().getCategoryPopupMenu();
        FeatureInstaller featInst = context.getFeatureInstaller();
        
        featInst.addPopupMenuItem(layerNamePopupMenu,
                this, this.getName() + "...", false,
                GUIUtil.toSmallIcon((ImageIcon) this.getIcon()),
                MoveCategoryToTop.createEnableCheck(context.getWorkbenchContext()));
    }
    
    
    
    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();
        
        multiEnableCheck.add( checkFactory.createAtLeastNCategoriesMustBeSelectedCheck(1) );
        multiEnableCheck.add( checkFactory.createExactlyNCategoriesMustBeSelectedCheck(1) );
        
        return multiEnableCheck;
	}

}
