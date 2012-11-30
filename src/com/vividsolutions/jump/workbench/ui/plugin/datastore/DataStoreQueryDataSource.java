package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;

import com.vividsolutions.jump.datastore.AdhocQuery;
import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.io.FeatureInputStream;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.model.cache.CachingFeatureCollection;
import com.vividsolutions.jump.workbench.model.cache.DynamicFeatureCollection;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.plugin.WorkbenchContextReference;

/**
 * Implements the DataSource interface in order to persist a query issued
 * from RunDatastoreQueryPlugIn.
 */
public class DataStoreQueryDataSource extends com.vividsolutions.jump.io.datasource.DataSource implements
        WorkbenchContextReference {

    public static final String DATASET_NAME_KEY = "Dataset Name";

    public static final String SQL_QUERY_KEY = "SQL Query";

    public static final String CONNECTION_DESCRIPTOR_KEY = "Connection Descriptor";
    
    private WorkbenchContext context;

    public DataStoreQueryDataSource() {
        // Called by Java2XML [Jon Aquino 2005-03-16]
    }
    
    /** Constructor used by the SaveToPostGISPlugIn.*/
    public DataStoreQueryDataSource(WorkbenchContext context) {
        this.context = context;
    }
    
    protected WorkbenchContext getWorkbenchContext() {
        return context;
    }

    public DataStoreQueryDataSource(String datasetName,
            String query,
            ConnectionDescriptor connectionDescriptor,
            WorkbenchContext context) {
        setProperties(CollectionUtil.createMap(new Object[] {
                DATASET_NAME_KEY, datasetName,
                SQL_QUERY_KEY, query,
                CONNECTION_DESCRIPTOR_KEY, connectionDescriptor}));
        setWorkbenchContext(context);
    }

    public boolean isWritable() {
        return false;
    }

    public Connection getConnection() {
        return new Connection() {
            public FeatureCollection executeQuery(String query,
                    Collection exceptions, TaskMonitor monitor) {
                try {
                    return createFeatureCollection();
                } catch (Exception e) {
                    exceptions.add(e);
                    return null;
                }
            }

            public FeatureCollection executeQuery(String query,
                    TaskMonitor monitor) throws Exception {
                Collection exceptions = new ArrayList();
                FeatureCollection featureCollection = executeQuery(query,
                        exceptions, monitor);
                if (!exceptions.isEmpty()) {
                    throw (Exception) exceptions.iterator().next();
                }
                return featureCollection;
            }

            public void executeUpdate(String query,
                    FeatureCollection featureCollection, TaskMonitor monitor)
                    throws Exception {
                throw new UnsupportedOperationException();
            }

            public void close() {
                // Do nothing, because DataStore connections are always
                // open (managed by the ConnectionManager). [Jon Aquino
                // 2005-03-16]
            }
        };
    }

    protected FeatureCollection createFeatureCollection() {
        FeatureInputStream featureInputStream = null;
        FeatureDataset featureDataset = null;
        
        ConnectionDescriptor connectionDescriptor =
            (ConnectionDescriptor)getProperties().get(CONNECTION_DESCRIPTOR_KEY);
        String query = (String)getProperties().get(SQL_QUERY_KEY);
        String driver = connectionDescriptor.getDataStoreDriverClassName();
        if (driver.contains("Postgis") && query.matches("(?s).*\\$\\{[^\\{\\}]*\\}.*")) {
            query = expandQuery(query, context.createPlugInContext());
        }
        
        try {
            featureInputStream = ConnectionManager.instance(context)
                .getOpenConnection(connectionDescriptor).execute(new AdhocQuery(query));
            featureDataset = new FeatureDataset(featureInputStream.getFeatureSchema());
            int i = 0;
            while (featureInputStream.hasNext()) {
                featureDataset.add( featureInputStream.next() );
            }
            return featureDataset;
        }
        catch(Exception e) {
            context.getWorkbench().getFrame().handleThrowable(e);
        }
        finally {
            if (featureInputStream != null) {
                try {featureInputStream.close();}
                catch(Exception e){}
            }
        }
        return featureDataset;
    }

    public void setWorkbenchContext(WorkbenchContext context) {
        this.context = context;
        try {
            // This method is called by OpenProjectPlugIn in the
            // GUI thread, so now is a good time to prompt for
            // a password if necessary. [Jon Aquino 2005-03-16]
            new PasswordPrompter().getOpenConnection(ConnectionManager
                    .instance(context),
                    (ConnectionDescriptor) getProperties().get(
                            CONNECTION_DESCRIPTOR_KEY), context.getWorkbench()
                            .getFrame());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected static String expandQuery(String query, PlugInContext context) {
        GeometryFactory gf = new GeometryFactory();
        Geometry viewG = gf.toGeometry(context.getLayerViewPanel().getViewport().getEnvelopeInModelCoordinates());
        Geometry fenceG = context.getLayerViewPanel().getFence();
        Geometry selectionG = selection(context);
        if (viewG != null) {
            query = query.replaceAll("\\$\\{view\\}", "\\${view:-1}");
            query = query.replaceAll("\\$\\{view(?::(-?[0-9]+))\\}", "ST_GeomFromText('" + viewG.toText() + "',$1)");
        }
        if (fenceG != null) {
            query = query.replaceAll("\\$\\{fence\\}", "\\${fence:-1}");
            query = query.replaceAll("\\$\\{fence(?::(-?[0-9]+))\\}", "ST_GeomFromText('" + fenceG.toText() + "',$1)");
        }
        else {
            query = query.replaceAll("\\$\\{fence\\}", "\\${fence:-1}");
            query = query.replaceAll("\\$\\{fence(?::(-?[0-9]+))\\}", "ST_GeomFromText('POLYGON EMPTY',$1)");
        }
        if (selectionG != null) {
            query = query.replaceAll("\\$\\{selection\\}", "\\${selection:-1}");
            query = query.replaceAll("\\$\\{selection(?::(-?[0-9]+))\\}", "ST_GeomFromText('" + selectionG.toText() + "',$1)");
        }
        return query;
    }
    
    private static Geometry selection(PlugInContext context) {
        Collection<Feature> features = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
        if (features.size() > 0) {
            if (features.size() == 1) {
                return features.iterator().next().getGeometry();
            }
            else {
                List<Geometry> selectedGeometries = new ArrayList<Geometry>();
                int max_dim = -1;
                for (Feature feature : features) {
                    int dim = feature.getGeometry().getDimension();
                    if (dim > max_dim) max_dim = dim;
                    selectedGeometries.add(feature.getGeometry());
                }
                List<Geometry> homogeneousGeometries = new ArrayList<Geometry>();
                for (Geometry g : selectedGeometries) {
                    if (g.getDimension() == max_dim) homogeneousGeometries.add(g);
                }
                Geometry union = UnaryUnionOp.union(homogeneousGeometries);
                if (!union.isEmpty()) return union;
                else return null;
            }
        }
        else return null;
    }

}
