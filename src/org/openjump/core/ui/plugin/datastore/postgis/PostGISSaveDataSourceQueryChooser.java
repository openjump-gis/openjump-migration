/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.openjump.core.ui.plugin.datastore.postgis;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooser;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

import org.openjump.core.ccordsys.srid.SRIDStyle;

/**
 * A DataSourceQueryChooser for writing to a PostGIS data source.
 */
public class PostGISSaveDataSourceQueryChooser implements DataSourceQueryChooser {
    
    public static final String KEY = PostGISSaveDataSourceQueryChooser.class.getName();
    
	static final String ERROR = I18N.get(KEY + ".error");
	static final String NO_CONNECTION_CHOOSEN     = I18N.get(KEY + ".no-connection-choosen");
	static final String NO_TABLE_CHOOSEN          = I18N.get(KEY + ".no-table-choosen");
	static final String CONNECTION_IS_NOT_POSTGIS = I18N.get(KEY + ".selected-connection-is-not-postgis");
	static final String UNIQUE_IDENTIFIER_NEEDED  = I18N.get(KEY + ".unique-identifier-is-needed");
    
	private PlugInContext context;
    private PostGISSaveDriverPanel panel;
    private SaveToPostGISDataSource dataSource;
    private HashMap properties;
    
    ButtonGroup methodButtons;
    JRadioButton insertButton;
    JRadioButton updateButton;
    JTextArea help;
    JTextField uniqueField;
  
    /**
     * Creates a new query chooser.
     * @param dataSource DataSource object to be queried against.
     */
    public PostGISSaveDataSourceQueryChooser(SaveToPostGISDataSource dataSource, PlugInContext context) {
        this.dataSource = dataSource;
        this.context = context;
        panel = new PostGISSaveDriverPanel(context);
        properties = new HashMap();
    }
  
    /**
     * @see com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooser#getComponent()
     */
    public Component getComponent() {
        //panel.changeTableName((String)properties.get(SaveToPostGISDataSource.TABLE_KEY));
        return panel; 
    }
  
    /**
     * Since the ui does not allow for loading of multiple tables, 
     * the returned collection only contains a single element.
     * @see com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooser#getDataSourceQueries()
     */
    public Collection getDataSourceQueries() {
        // Get the name of the table to update 
        String updateQuery = (String)properties.get(SaveToPostGISDataSource.TABLE_KEY);
        // Create a DataSourceQuery from a datasource, and a query   
        SaveToPostGISDataSourceQuery query = new SaveToPostGISDataSourceQuery(
            getDataSource(), updateQuery, 
            (String)properties.get(SaveToPostGISDataSource.TABLE_KEY)
        );    
        query.setProperties(getProperties());
        List queries = new ArrayList();
        queries.add(query);
      
        return queries;
    }
    
  
    /**
     * Checks that user input is valid.
     * @see com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooser#isInputValid()
     */
    public boolean isInputValid() {
        if (panel.getConnectionDescriptor() == null) { 
                JOptionPane.showMessageDialog(panel, 
                    NO_CONNECTION_CHOOSEN,
                    ERROR, JOptionPane.ERROR_MESSAGE );
            return false;
        }
        else if (!panel.getConnectionDescriptor()
                       .getDataStoreDriverClassName()
                       .equals("com.vividsolutions.jump.datastore.postgis.PostgisDataStoreDriver")) {
                JOptionPane.showMessageDialog(null,
                    CONNECTION_IS_NOT_POSTGIS,
                    ERROR, JOptionPane.ERROR_MESSAGE );
            return false;
        }
        else if (panel.getTableName() == null || 
            panel.getTableName().trim().length() == 0) { 
                JOptionPane.showMessageDialog(panel, 
                    NO_TABLE_CHOOSEN,
                    ERROR, JOptionPane.ERROR_MESSAGE );
            return false;
        }
        else if ((panel.getLocalId() == null || panel.getLocalId().trim().length() == 0) && 
            (panel.getSaveMethod().equals(SaveToPostGISDataSource.SAVE_METHOD_UPDATE) || 
             panel.getSaveMethod().equals(SaveToPostGISDataSource.SAVE_METHOD_DELETE))) { 
                JOptionPane.showMessageDialog(panel, 
                    UNIQUE_IDENTIFIER_NEEDED,
                    ERROR, JOptionPane.ERROR_MESSAGE );
            return false;
        }
        // put the TABLE_KEY value early to make sure it will appear in the 
        // monitor see also AbstractSaveDatasetAsPlugIn 
        properties.put(SaveToPostGISDataSource.TABLE_KEY, panel.getTableName());
        return true;
    }
  
    /**
     * Reads all the connection + query properties from the ui.
     */
    protected HashMap getProperties() {
        if (properties == null) properties = new HashMap();
        properties.put(SaveToPostGISDataSource.CONNECTION_DESCRIPTOR_KEY, panel.getConnectionDescriptor());
        properties.put(SaveToPostGISDataSource.TABLE_KEY, panel.getTableName());
        properties.put(SaveToPostGISDataSource.SAVE_METHOD_KEY, panel.getSaveMethod());
        properties.put(SaveToPostGISDataSource.LOCAL_ID_KEY, panel.getLocalId());
        properties.put(SaveToPostGISDataSource.USE_DB_ID_KEY, panel.isCreateDbIdColumnSelected());
        Layer[] layers = context.getWorkbenchContext().getLayerNamePanel().getSelectedLayers();
        if (layers.length == 1) {
            properties.put(SaveToPostGISDataSource.DATASET_NAME_KEY, layers[0].getName());
            FeatureSchema schema = layers[0].getFeatureCollectionWrapper().getFeatureSchema();
            String[] schema_table = PostGISQueryUtil.divideTableName(panel.getTableName());
            
            properties.put(SaveToPostGISDataSource.SQL_QUERY_KEY, "SELECT * FROM " +
                PostGISQueryUtil.compose(schema_table[0], schema_table[1]) + 
                " LIMIT 100000");
            // OpenJUMP has now a better support of Coordinate System at
            // FeatureCollection and FeatureSchema level, but this one is simple
            // and makes it easy to set the SRID the user want before an update
            SRIDStyle sridStyle = (SRIDStyle) layers[0].getStyle(SRIDStyle.class);
            properties.put(SaveToPostGISDataSource.SRID_KEY, sridStyle.getSRID()); 
            //properties.put(SaveToPostGISDataSource.MAX_FEATURES_KEY, 100000);
            //properties.put(SaveToPostGISDataSource.WHERE_CLAUSE_KEY, "");
            //properties.put(SaveToPostGISDataSource.CACHING_KEY, false);
        }
        return properties;
    }
    
    protected SaveToPostGISDataSource getDataSource() {
        return dataSource;   
    }
  
    /**
     * Returns the String displayed in the Format Chooser.
     */
    public String toString() {
        return "PostGIS Table (new)";
    }

}