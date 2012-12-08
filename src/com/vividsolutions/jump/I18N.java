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
package com.vividsolutions.jump;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

/**
 * Singleton for the Internationalization (I18N)
 * 
 * <pre>
 * [1] HOWTO TRANSLATE JUMP IN MY OWN LANGUAGE
 *  Copy theses files and add the locales extension for your language and country instead of the *.
 *  - resources/jump_*.properties
 *  - com/vividsolutions/jump/workbench/ui/plugin/KeyboardPlugIn_*.html
 * [2] HOWTO TRANSLATE MY PLUGIN AND GIVE THE ABILITY TO TRANSLATE IT
 *  Use theses methods to use your own *.properties files :
 *  [Michael Michaud 2007-03-23] the 3 following methods have been deactivated
 *  com.vividsolutions.jump.I18N#setPlugInRessource(String, String)
 *  com.vividsolutions.jump.I18N#get(String, String)
 *  com.vividsolutions.jump.I18N#getMessage(String, String, Object[])
 *  you still can use plugInsResourceBundle (as in Pirol's plugin)
 *  
 *  And use jump standard menus
 * </pre>
 * 
 * Code example : [Michael Michaud 2007-03-23 : the following code example is no
 * more valid and has to be changed]
 * 
 * <pre>
 * public class PrintPlugIn extends AbstractPlugIn
 *  {
 *    private String name = &quot;print&quot;;
 * public void initialize(PlugInContext context) throws Exception
 * {
 *   I18N.setPlugInRessource(name, &quot;org.agil.core.jump.plugin.print&quot;);
 *   context.getFeatureInstaller().addMainMenuItem(this,
 *                                                 new String[]
 *                                                 {MenuNames.TOOLS,I18N.get(name, &quot;print&quot;)},
 *                                                 I18N.get(name, &quot;print&quot;), false, null, null);
 * }
 * ...
 * </pre>
 * 
 * <pre>
 * TODO :I18N (1) Improve translations
 * TODO :I18N (2) Separate config (customization) and I18N
 * TODO :I18N (3) Explore and discuss about I18N integration and Jakarta Common Ressources
 * (using it as a ressource interface)
 * </pre>
 * 
 * @author Basile Chandesris - <chandesris@pt-consulting.lu>
 * @see com.vividsolutions.jump.workbench.ui.MenuNames
 * @see com.vividsolutions.jump.workbench.ui.VTextIcon text rotation
 */
public final class I18N {

  private static final Logger LOG = Logger.getLogger(I18N.class);
  private static final I18N instance = new I18N();

  // [Michael Michaud 2007-03-23] plugInsResourceBundle is deactivated because
  // all the methods
  // using it have been deactivated.
  // [sstein] activated again since Pirol does use it
  // [ede 11.2012] kept it, although i don't see any usage of it in here
  public static Hashtable plugInsResourceBundle = new Hashtable();
  /** The map from category names to I18N instances. */
  private static Map<String, I18N> instances = new HashMap<String, I18N>();

  /** The defaults for the I18N instance. */
  private static ClassLoader classLoader;
  private String resourcePath = "language/jump";
  private Locale locale = Locale.getDefault();
  /** three rbs see getText(String) for details */
  private ResourceBundle resourceBundle,resourceBundle2,resourceBundle3;

  private I18N() { init(); }

  /**
   * Construct an I18N instance for the category.
   * 
   * @param resourcePath The path to the language files.
   */
  private I18N( final String category ) {
    resourcePath = category.replace('.', '/') + "/" + resourcePath;
    init();
  }

  private void locale(Locale loc) {
    if (loc != null) {
      locale = loc;
      init();
    }
  }

  /**
   * the following three methods locale() language() country() are named that
   * way because the getter methods were already defined statically and have
   * to remain for legacy reasons
   */
  private Locale locale() {
    return locale != null ? locale : Locale.getDefault();
  }
  private String language() {
    return locale().getLanguage();
  }
  private String country() {
    return locale().getCountry();
  }

  // everytime something important changes the resourcebundles have to be
  // recreated accordingly and the runtime should be updated as well
  private void init() {
    ClassLoader cl = classLoader instanceof ClassLoader ? classLoader
        : getClass().getClassLoader();
    // load resourcebundle accordingly
    resourceBundle = ResourceBundle.getBundle(resourcePath, locale, cl);
    resourceBundle2 = ResourceBundle.getBundle(resourcePath, new Locale(
        language()), cl);
    resourceBundle3 = ResourceBundle.getBundle(resourcePath, Locale.ROOT, cl);
    // apply to system
    applyToRuntime(locale);
  }

  /**
   * Get the I18N text from the language file associated with this instance. If
   * no label is defined then a default string is created from the last part of
   * the key.
   * 
   * ATTENTION: using three resource bundles is a workaround to enable having
   * entries in the language properties that are not translated now (empty or
   * #T:something)
   * 
   * @param key
   *          The key of the text in the language file.
   * @return The I18Nized text.
   */
  public String getText(final String key) {
    String text;
    try {
      // try lang_country resourcebundle
      if (isValid(text = resourceBundle.getString(key)))
        return text;
      // try language only resourcebundle
      if (isValid(text = resourceBundle2.getString(key)))
        return text;
      // eventually use base resourcebundle
      return resourceBundle3.getString(key);
    } catch (java.util.MissingResourceException e) {
      String[] labelpath = key.split("\\.");
      String msg = getClass().getName()+"\nNo resource bundle or no translation found for''{0}''.\nError was:\n{1}";
      msg = new MessageFormat(msg).format(new String[]{key,e.getLocalizedMessage()});
      LOG.debug(msg);
      System.out.println("Missing translation for '"+key+"' in resource bundle '"+this.resourcePath+"'.");
      return labelpath[labelpath.length - 1];
    }
  }

  private boolean isValid( String text ){
    return text != null && !text.trim().isEmpty() && !text.trim().startsWith("#T:");
  }

  /**
   * Set the class loader used to load resource bundles, must be called by the
   * PlugInManager (plugin jars are added to a child classloader there) to allow
   * plugins to make use of this I18N class.
   * 
   * @param classLoader
   *          the classLoader to set
   */
  public static void setClassLoader(ClassLoader cl) {
    if (cl!=null) classLoader = cl;
    // apply to instances
    for (I18N i18n : instances.values()) {
      i18n.init();
    }
    getInstance().init();
  }

  /**
   * Get the I18N text from the language file associated with the specified
   * category. If no label is defined then a default string is created from the
   * last part of the key.
   * 
   * @param category The category.
   * @param key The key of the text in the language file.
   * @return The I18Nized text.
   */
  public static String getText(final String category, final String key) {
    I18N i18n = getInstance(category);
    return i18n.getText(key);
  }

  /**
   * Get the I18N instance for the category. A resource file must exist in the
   * resource path for language/jump for the category.
   * 
   * @param category The category.
   * @return The instance.
   */
  public static I18N getInstance(final String category) {
    I18N instance = instances.get(category);
    if (instance == null) {
      instance = new I18N( category );
      instances.put(category, instance);
    }
    return instance;
  }

  public static I18N getInstance() {
    // [Michael Michaud 2007-03-04] guarantee I18N instance unicity without
    return (instance == null) ? new I18N() : instance;
  }

  // [ede] utility method as it is used in several places (loadFile,getLanguage...)
  public static Locale fromCode(final String localeCode) {
    // [Michael Michaud 2007-03-04] handle the case where lang is the only
    // variable instead of catching an ArrayIndexOutOfBoundsException
    String[] lc = localeCode.split("_");
    Locale locale = Locale.getDefault();
    if (lc.length > 1) {
      LOG.debug("lang:" + lc[0] + " " + "country:" + lc[1]);
      locale = new Locale(lc[0], lc[1]);
    } else if (lc.length > 0) {
      LOG.debug("lang:" + lc[0]);
      locale = new Locale(lc[0]);
    } else {
      LOG.debug(localeCode
        + " is an illegal argument to define lang [and country]");
    }
    
    return locale;
  }
  
  /**
   * Load file specified in command line (-i18n lang_country) (lang_country
   * :language 2 letters + "_" + country 2 letters) Tries first to extract lang
   * and country, and if only lang is specified, loads the corresponding
   * resource bundle.
   * 
   * @param langcountry
   */
  public static void setLocale(final String langcountry) {
    Locale loc = fromCode(langcountry);
    getInstance().locale(loc);
    getInstance().init();
  }

  /***
   * Applies a given locale to the java runtime.
   * 
   * @param locale
   */
  public static void applyToRuntime( Locale loc ) {
    Locale.setDefault(loc);
    System.setProperty("user.language", loc.getLanguage());
    System.setProperty("user.country", loc.getCountry());
  }

  /**
   * Process text with the locale 'jump_<locale>.properties' file
   * 
   * @param label
   * @return i18n label [Michael Michaud 2007-03-23] If no resourcebundle is
   *         found, returns a default string which is the last part of the label
   */
  public static String get(final String label) {
    return getInstance().getText(label);
  }

  /**
   * Get the short signature for locale (letters extension :language 2 letters +
   * "_" + country 2 letters)
   * 
   * @return string signature for locale
   */
  public static String getLocale() {
    return getLanguage() + "_" + getCountry();
  }

  /**
   * Get the short signature for language (letters extension :language 2
   * letters) of the default instance
   * 
   * @return string signature for language
   */
  public static String getLanguage() {
    return getInstance().language();
  }

  /**
   * Get the short signature for country (2 letter code) of the default instance
   * 
   * @return string signature for country
   */
  public static String getCountry() {
    return getInstance().country();
  }

  /**
   * Process text with the locale 'jump_<locale>.properties' file If no
   * resourcebundle is found, returns default string contained inside
   * com.vividsolutions.jump.jump
   * 
   * @param label with argument insertion : {0}
   * @param objects
   * @return i18n label
   */
  public static String getMessage(final String label, final Object[] objects) {
    return getMessage("", label, objects);
  }

  // convenience method for one parameter only calls
  public static String getMessage(final String label, final Object object) {
    return getMessage("", label, new Object[]{object});
  }

  /**
   * Get the I18N text from the language file associated with the specified
   * category. If no label is defined then a default string is created from the
   * last part of the key.
   * 
   * @param category
   *          The category.
   * @param label
   *          Label with argument insertion : {0}
   * @param objects
   * @return i18n label
   */
  public static String getMessage(final String category, final String label,
      final Object[] objects) {
    I18N i18n = !category.trim().isEmpty() ? getInstance(category)
        : getInstance();
    try {
      final MessageFormat mformat = new MessageFormat(i18n.getText(label));
      return mformat.format(objects);
    } catch (java.util.MissingResourceException e) {
      final String[] labelpath = label.split("\\.");
      LOG.warn(e.getMessage() + " no default value, the resource key is used: "
          + labelpath[labelpath.length - 1]);
      final MessageFormat mformat = new MessageFormat(
          labelpath[labelpath.length - 1]);
      return mformat.format(objects);
    }
  }

}

