/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
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
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.wms;

import static javax.swing.JOptionPane.NO_OPTION;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.*;

import org.openjump.util.UriUtil;

import com.vividsolutions.jump.I18N;

/**
 * Represents a remote WMS Service.
 *
 * @author Chris Hodgson chodgson@refractions.net
 */
public class WMService {

  public static final String WMS_1_0_0 = "1.0.0";

  public static final String WMS_1_1_0 = "1.1.0";

  public static final String WMS_1_1_1 = "1.1.1";

  public static final String WMS_1_3_0 = "1.3.0";

  private URL serverUrl;
  private String wmsVersion = WMS_1_1_1;
  private Capabilities cap;
  // true if the user just confirm that he accepts or not untrusted connexion

  /**
   * Constructs a WMService object from a server URL.
   * 
   * @param serverUrl
   *          the URL of the WMS server
   * @param wmsVersion
   *          the WMS version
   */
  public WMService(String serverUrl, String wmsVersion) {
    try {
      this.serverUrl = new URL(serverUrl);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }

    if (wmsVersion != null)
      this.wmsVersion = wmsVersion;
    this.cap = null;
  }

  /**
   * Constructs a WMService object from a server URL.
   * 
   * @param serverUrl
   *          the URL of the WMS server
   */
  public WMService(String serverUrl) {
    this(serverUrl, null);
  }

  /**
   * @throws IOException if an error occurs during connection initialization
   */
  public void initialize() throws IOException {
    initialize(false);
  }

  public String getWmsVersion() {
    return wmsVersion;
  }

  /**
   * Connect to the service and get the capabilities. This must be called before
   * anything else is done with this service.
   * 
   * @param alertDifferingURL
   *          alert the user if a different GetMap URL is available
   * @throws IOException if an error occurs during connection initialization
   */
  public void initialize(boolean alertDifferingURL) throws IOException {
    // [UT]
    String req = "request=capabilities&WMTVER=1.0";
    IParser parser = new ParserWMS1_1();
    if (WMS_1_0_0.equals(wmsVersion)) {
      req = "SERVICE=WMS&VERSION=1.0.0&REQUEST=GetCapabilities";
      parser = new ParserWMS1_0();
    } else if (WMS_1_1_0.equals(wmsVersion)) {
      req = "SERVICE=WMS&VERSION=1.1.0&REQUEST=GetCapabilities";
      parser = new ParserWMS1_1();
    } else if (WMS_1_1_1.equals(wmsVersion)) {
      req = "SERVICE=WMS&VERSION=1.1.1&REQUEST=GetCapabilities";
      parser = new ParserWMS1_1();
    } else if (WMS_1_3_0.equals(wmsVersion)) {
      req = "SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities";
      parser = new ParserWMS1_3();
    }

    try {
      String requestUrlString = WMService.legalize(this.serverUrl.toString()) + req;
      URL requestUrl = new URL(requestUrlString);

      InputStream inputStream = new BasicRequest(this, requestUrl).getInputStream();
      cap = parser.parseCapabilities(this, inputStream);
      String url1 = cap.getService().getServerUrl();
      String url2 = cap.getGetMapURL();

      String compare_url1 = UriUtil.urlStripAuth(legalize(url1));
      String compare_url2 = UriUtil.urlStripAuth(legalize(url2));
      // if the difference is only in credentials then use url1 else ask from
      // user
      if (!compare_url1.equals(compare_url2) && alertDifferingURL) {
        int resp = showConfirmDialog(
                null,
                I18N.getMessage("com.vididsolutions.wms.WMService.Other-GetMap-URL-Found", url2),
                null,
                YES_NO_OPTION);
        // nope. user wants to keep the initial url
        if (resp == NO_OPTION) {
          cap.setGetMapURL(url1);
        }
        // make sure url2 has auth info if needed
        else if (!UriUtil.urlGetUser(url1).isEmpty()) {
          String url2_withAuth = UriUtil.urlAddCredentials(url2,
                  UriUtil.urlGetUser(url1), UriUtil.urlGetPassword(url1));
          cap.setGetMapURL(url2_withAuth);
        }
      } else {
        // changed 24.06.2011 (Wilfried Hornburg, LGLN) url1 --> url2; original:
        // cap.setGetMapURL(url1);
        // revert to url1, following Jukka's advice a discussion is on-going on
        // JPP mailing list
        cap.setGetMapURL(url1);
      }
    } catch(SSLHandshakeException ex) {
      /*
      int r = JOptionPane.showConfirmDialog(
              null,
              I18N.getMessage("com.vididsolutions.wms.WMService.UnverifiedCertificate",
                      // create a new URL to hide user/password
                      new URL(serverUrl.getProtocol(), serverUrl.getHost(), serverUrl.getPort(), serverUrl.getFile())
              ),
              "Confirmation dialog",
              YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE);

      if (r==JOptionPane.YES_OPTION) {
        try {
          setTrustOption(true, serverUrl);
        } catch(KeyManagementException|NoSuchAlgorithmException ex2) {
          throw new IOException(ex2);
        }
        initialize(alertDifferingURL);
      } else throw new IOException(ex);
      */
      throw new IOException(ex);
    }

    // [2016.01 ede] deactivated the error handling here as it leads to an
    // infinite stack loop when trying to open a project containing a wms layer
    // that can't be connected for some reason show error, close errordialog,
    // new render of taskframe, show error...
    //    } catch (FileNotFoundException e) {
//      JOptionPane.showMessageDialog(null, I18N.getMessage(
//          "com.vividsolutions.wms.WMService.WMS-Not-Found",
//          new Object[] { e.getLocalizedMessage() }), I18N
//          .get("com.vividsolutions.wms.WMService.Error"),
//          JOptionPane.ERROR_MESSAGE);
//      throw e;
//    } catch (final WMSException e) {
//      ErrorDialog.show(null, "WMS Error", e.getMessage(), e.getSource());
//      throw e;
//    } catch (IOException e) {
//      JOptionPane.showMessageDialog(null, I18N.getMessage(
//          "com.vividsolutions.wms.WMService.WMS-IO-Error", new Object[] {
//              e.getClass().getSimpleName(), e.getLocalizedMessage() }), I18N
//          .get("com.vividsolutions.wms.WMService.Error"),
//          JOptionPane.ERROR_MESSAGE);
//      throw e;
//    }
  }

  //private TrustManager trm = new X509TrustManager() {
  //  public X509Certificate[] getAcceptedIssuers() { return null; }
  //  public void checkClientTrusted(X509Certificate[] certs, String authType) { }
  //  public void checkServerTrusted(X509Certificate[] certs, String authType) { }
  //};
  //private Set<URL> trustedURLs = new HashSet<>();

  //private void setTrustOption(boolean trust, URL url)
  //        throws KeyManagementException, NoSuchAlgorithmException {
  //  SSLContext sc = SSLContext.getInstance("SSL");
  //  if (trust || trustedURLs.contains(url)) {
  //    sc.init(null, new TrustManager[]{trm}, null);
  //    trustedURLs.add(url);
  //  } else {
  //    sc.init(null, null, null);
  //  }
  //  HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
  //}

  /**
   * Gets the url stringof the map service.
   * 
   * @return the url of the WMService
   */
  public String getServerUrl() {
    return serverUrl.toString();
  }

  /**
   * Gets the url object of the map service. Added as the getServerUrl() was
   * there earlier for backward compatibility.
   * 
   * @return the url of the WMService
   */
  public URL getServerUrlAsUrl() {
    return serverUrl;
  }

  /**
   * Gets the title of the map service. The service must have previously been
   * initialized, otherwise null is returned.
   * 
   * @return the title of the WMService
   */
  public String getTitle() {
    return cap.getTitle();
  }

  /**
   * Gets the Capabilities for this service. The service must have previously
   * been initialized, otherwise null is returned.
   * 
   * @return a copy of the MapDescriptor for this service
   */
  public Capabilities getCapabilities() {
    return cap;
  }

  /**
   * Creates a new MapRequest object which can be used to retrieve a Map from
   * this service.
   * 
   * @return a MapRequest object which can be used to retrieve a map image from
   *         this service
   */
  public MapRequest createMapRequest() {
    // [UT] 04.02.2005 changed
    MapRequest mr = new MapRequest(this);
    mr.setWMSVersion(this.wmsVersion);
    return mr;
  }

  public String getVersion() {
    return wmsVersion;
  }

  //
  // The WMService appends other parameters to the end of the URL
  //
  public static String legalize(String url) {
    String fixedURL = url.trim();

    if (!fixedURL.contains("?")) {
      fixedURL = fixedURL + "?";
    } else {
      if (fixedURL.endsWith("?")) {
        // ok
      } else {
        // it must have other parameters
        if (!fixedURL.endsWith("&")) {
          fixedURL = fixedURL + "&";
        }
      }
    }

    return fixedURL;
  }
}
