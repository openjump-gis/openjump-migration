/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ugo Taddei (taddei@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */

package de.latlon.deejump.wfs.client;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.deegree.datatypes.QualifiedName;
import org.deegree.framework.xml.DOMPrinter;
import org.deegree.framework.xml.XMLFragment;
import org.deegree.framework.xml.XMLParsingException;
import org.deegree.framework.xml.XMLTools;
import org.deegree.ogcbase.CommonNamespaces;
import org.deegree.ogcwebservices.wfs.capabilities.WFSFeatureType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.latlon.deejump.wfs.auth.UserData;

/**
 * The WFS 1.0.0 client.
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version $Revision: 1438 $, $Date: 2008-05-29 15:00:02 +0200 (Do, 29 Mai 2008) $
 */
public class WFServiceWrapper_1_0_0 extends AbstractWFSWrapper {

    private XMLFragment capsDoc;

    private String[] featureTypes;

    /**
     * @param logins
     * @param baseUrl
     */
    public WFServiceWrapper_1_0_0( UserData logins, String baseUrl ) {
        super( logins, baseUrl );
        init();
    }

    private void init() {

        capsDoc = new de.latlon.deejump.wfs.client.deegree2mods.XMLFragment();

        try {
            capsDoc.load( new URL( getCapabilitiesURL() ) );
        } catch ( Exception e ) {
            e.printStackTrace();
        }

    }

    @Override
    public String getCapabilitesAsString() {
        return DOMPrinter.nodeToString( capsDoc.getRootElement(), " " );
    }

    @Override
    public synchronized String[] getFeatureTypes() {
        if ( featureTypes == null ) {
            featureTypes = extractFeatureTypes();
        }
        return featureTypes;
    }

    private synchronized String[] extractFeatureTypes() {

        String[] fts = null;

        ftNameToWfsFT = new HashMap<String, WFSFeatureType>();

        Element root = this.capsDoc.getRootElement();

        try {
            List<Element> nodes = XMLTools.getElements( root, "wfs:FeatureTypeList/wfs:FeatureType",
                                                        CommonNamespaces.getNamespaceContext() );

            List<String> ftList = new ArrayList<String>( nodes.size() );
            for ( Element n : nodes ) {
                Node node = XMLTools.getNode( n, "wfs:Name/text()", CommonNamespaces.getNamespaceContext() );

                QualifiedName qualiName = XMLFragment.parseQualifiedName( node );

                ftList.add( qualiName.getLocalName() );

                URI uri = XMLTools.getNodeAsURI( n, "wfs:SRS/text()", CommonNamespaces.getNamespaceContext(), null );
                WFSFeatureType wfsFt = new WFSFeatureType( qualiName, null, null, null, uri, null, null, null, null,
                                                           null );

                ftNameToWfsFT.put( qualiName.getLocalName(), wfsFt );

            }
            fts = ftList.toArray( new String[ftList.size()] );
        } catch ( XMLParsingException e ) {
            e.printStackTrace();
        }
        return fts;

    }

    private String createOnlineResourceForOperation( String operationName, String httpMeth ) {
        String value = null;
        Element root = this.capsDoc.getRootElement();

        try {
            value = XMLTools.getRequiredNodeAsString( root, "wfs:Capability/wfs:Request/wfs:" + operationName
                                                            + "/wfs:DCPType/wfs:HTTP/wfs:" + httpMeth
                                                            + "/@onlineResource",
                                                      CommonNamespaces.getNamespaceContext() );

        } catch ( XMLParsingException e ) {
            e.printStackTrace();
        }
        return value;

    }

    @Override
    public String createDescribeFTOnlineResource() {
        return createOnlineResourceForOperation( "DescribeFeatureType", "Get" );
    }

    @Override
    public String getServiceVersion() {
        return "1.0.0";
    }

    @Override
    public String getGetFeatureURL() {
        return createOnlineResourceForOperation( "GetFeature", "Post" );
    }

}
