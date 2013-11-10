package org.openjump.core.ui.plugin.datastore.postgis2;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooser;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import org.openjump.core.ccordsys.srid.SRIDStyle;
import org.openjump.core.ui.plugin.datastore.WritableDataStoreDataSource;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A DataSourceQueryChooser for writing to a PostGIS data source.
 */
public class PostGISSaveDataSourceQueryChooser implements DataSourceQueryChooser {

    public static final String KEY = PostGISSaveDataSourceQueryChooser.class.getName();

    static final String ERROR = I18N.get(KEY + ".error");
    static final String NO_CONNECTION_CHOOSEN     = I18N.get(KEY + ".no-connection-choosen");
    static final String NO_TABLE_CHOOSEN          = I18N.get(KEY + ".no-table-choosen");
    static final String CONNECTION_IS_NOT_POSTGIS = I18N.get(KEY + ".selected-connection-is-not-postgis");

    private PlugInContext context;
    private PostGISSaveDriverPanel panel;
    private WritableDataStoreDataSource dataSource;
    private Map<String,Object> properties;

    /**
     * Creates a new query chooser.
     * @param dataSource DataSource object to be queried against.
     */
    public PostGISSaveDataSourceQueryChooser(WritableDataStoreDataSource dataSource, PlugInContext context) {
        this.dataSource = dataSource;
        this.context = context;
        panel = new PostGISSaveDriverPanel(context);
        properties = new HashMap<String,Object>();
    }

    /**
     * @see com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooser#getComponent()
     */
    public Component getComponent() {
        return panel;
    }

    /**
     * Since the ui does not allow for loading of multiple tables,
     * the returned collection only contains a single element.
     * @see com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooser#getDataSourceQueries()
     */
    public Collection getDataSourceQueries() {
        // Get the name of the table to update
        String updateQuery = (String)properties.get(WritableDataStoreDataSource.DATASET_NAME_KEY);
        // Create a DataSourceQuery from a datasource, and a query
        SaveToPostGISDataSourceQuery query = new SaveToPostGISDataSourceQuery(
                getDataSource(), updateQuery,
                (String)properties.get(WritableDataStoreDataSource.DATASET_NAME_KEY)
        );
        query.setProperties(getProperties());
        List<DataSourceQuery> queries = new ArrayList<DataSourceQuery>();
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
                    ERROR, JOptionPane.ERROR_MESSAGE);
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

        // else if ((panel.getPrimaryKey() == null || panel.getPrimaryKey().trim().length() == 0) &&
        //         (panel.getSaveMethod().equals(RWDataStoreDataSource.SAVE_METHOD_UPDATE) ||
        //                 panel.getSaveMethod().equals(RWDataStoreDataSource.SAVE_METHOD_DELETE))) {
        //     JOptionPane.showMessageDialog(panel,
        //             UNIQUE_IDENTIFIER_NEEDED,
        //             ERROR, JOptionPane.ERROR_MESSAGE );
        //     return false;
        // }

        // put the TABLE_KEY value early to make sure it will appear in the
        // monitor see also AbstractSaveDatasetAsPlugIn
        properties.put(WritableDataStoreDataSource.DATASET_NAME_KEY, panel.getTableName());
        properties.put(WritableDataStoreDataSource.CREATE_PK, panel.isCreatePrimaryKeyColumnSelected());
        return true;
    }

    /**
     * Reads all the connection + query properties from the ui.
     */
    protected Map getProperties() {
        if (properties == null) properties = new HashMap<String,Object>();
        properties.put(WritableDataStoreDataSource.CONNECTION_DESCRIPTOR_KEY, panel.getConnectionDescriptor());
        properties.put(WritableDataStoreDataSource.DATASET_NAME_KEY, panel.getTableName());
        //properties.put(RWDataStoreDataSource.SAVE_METHOD_KEY, panel.getSaveMethod());
        //properties.put(RWDataStoreDataSource.PRIMARY_KEY, panel.getPrimaryKey());
        properties.put(WritableDataStoreDataSource.CREATE_PK, panel.isCreatePrimaryKeyColumnSelected());
        if (panel.isCreatePrimaryKeyColumnSelected()) {
            properties.put(WritableDataStoreDataSource.EXTERNAL_PK_KEY, WritableDataStoreDataSource.DEFAULT_PK_NAME);
        }
        Layer[] layers = context.getWorkbenchContext().getLayerNamePanel().getSelectedLayers();
        if (layers.length == 1) {
            //properties.put(RWDataStoreDataSource.DATASET_NAME_KEY, layers[0].getName());
            FeatureSchema schema = layers[0].getFeatureCollectionWrapper().getFeatureSchema();
            //String[] schema_table = PostGISQueryUtil.splitTableName(panel.getTableName());
            properties.put(WritableDataStoreDataSource.GEOMETRY_ATTRIBUTE_NAME_KEY,
                    schema.getAttributeName(schema.getGeometryIndex()));

            //properties.put(RWDataStoreDataSource.SQL_QUERY_KEY, "SELECT * FROM " +
            //        PostGISQueryUtil.compose(schema_table[0], schema_table[1]) + " LIMIT 100000");

            // OpenJUMP has now a better support of Coordinate System at
            // FeatureCollection and FeatureSchema level, but this one is simple
            // and makes it easy to set the SRID the user want before an update
            SRIDStyle sridStyle = (SRIDStyle)layers[0].getStyle(SRIDStyle.class);
            properties.put(WritableDataStoreDataSource.SRID_KEY, sridStyle.getSRID());
            //properties.put(SaveToPostGISDataSource.MAX_FEATURES_KEY, 100000);
            //properties.put(SaveToPostGISDataSource.WHERE_CLAUSE_KEY, "");
            //properties.put(SaveToPostGISDataSource.CACHING_KEY, false);
        }
        properties.put(WritableDataStoreDataSource.CREATE_TABLE, true);
        properties.put(WritableDataStoreDataSource.LIMITED_TO_VIEW, false);
        properties.put(WritableDataStoreDataSource.MAX_FEATURES_KEY, Integer.MAX_VALUE);
        properties.put(WritableDataStoreDataSource.MANAGE_CONFLICTS, true);
        return properties;
    }

    protected WritableDataStoreDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Returns the String displayed in the Format Chooser.
     */
    public String toString() {
        return I18N.get(KEY + ".PostGIS-Table");
    }
}