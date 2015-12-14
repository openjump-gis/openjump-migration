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

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

/**
 * Singleton for the Internationalization (I18N)
 **/
public final class I18N {

  private static final Logger LOG = Logger.getLogger(I18N.class);
  private static final I18N instance = new I18N();

  // [Michael Michaud 2007-03-23] plugInsResourceBundle is deactivated because
  // all the methods using it have been deactivated.
  // [sstein] activated again since Pirol does use it
  // [ede 11.2012] kept it, although i don't see any usage of it in here
  // [ede 12.2015] used by de.fho.jump.pirol.utilities.i18n.I18NPlug
  public static Hashtable plugInsResourceBundle = new Hashtable();
  /** The map of I18N instances. */
  private static Map<Object, I18N> instances = new HashMap<Object, I18N>();

  /** The defaults for the I18N instance. */
  private static ClassLoader classLoader;
  private String resourcePath = "language/jump";
  private Locale locale = Locale.getDefault();
  /** three rbs see getText(String) for details */
  private ResourceBundle resourceBundle, resourceBundle2, resourceBundle3;

  private I18N() {
    init();
  }

  /**
   * Construct an I18N instance for the category.
   * 
   * @param categoryPrefix
   *          i18n files should be in category/language/jump files.
   */
  private I18N(final String categoryPrefix) {
    resourcePath = categoryPrefix.replace('.', '/') + "/" + resourcePath;
    init();
  }

  /**
   * Create an instance for a concrete path without 'language/jump' appended
   * 
   * @param path
   */
  private I18N(final File path) {
    resourcePath = path.toString();
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
   * way because the getter methods were already defined statically and have to
   * remain for legacy reasons
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
    resourceBundle3 = ResourceBundle.getBundle(resourcePath,
        new Locale("", ""), cl);
    // apply to system
    applyToRuntime(locale);
  }

  // remember missing strings, do not flood log
  private HashSet missing = new HashSet();

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
      if (!missing.contains(key)) {
        String msg = getClass().getName()
            + "\nNo resource bundle or no translation found for''{0}''.\nError was:\n{1}";
        msg = new MessageFormat(msg).format(new String[] { key,
            e.getLocalizedMessage() });
        LOG.debug(msg);
        System.out.println("Missing translation for '" + key
            + "' in resource bundle '" + this.resourcePath + "'.");
        missing.add(key);
        // uncomment and add a search string to get staks telling you where the
        // call came from
        // Assert.isTrue(!key.contains("Write"));
      }
      String[] labelpath = key.split("\\.");
      return labelpath[labelpath.length - 1];
    }
  }

  /**
   * we ignore untranslated string when we find them
   * 
   * @param text
   * @return
   */
  private boolean isValid(String text) {
    return text != null && !text.trim().equals("")
        && !text.trim().startsWith("#T:");
  }

  /**
   * Set the class loader used to load resource bundles, must be called by the
   * PlugInManager (plugin jars are added to a child classloader there) to allow
   * plugins to make use of this I18N class.
   * 
   * @param cl
   *          the classLoader to set
   */
  public static void setClassLoader(ClassLoader cl) {
    if (cl != null)
      classLoader = cl;
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
   * @deprecated use getMessage() instead
   * @param categoryPrefix
   *          The category.
   * @param key
   *          The key of the text in the language file.
   * @return The I18Nized text.
   */
  public static String getText(final String categoryPrefix, final String key) {
    return getMessage(categoryPrefix, key);
  }

  /**
   * Get the I18N instance for a category or a path. The resource files are
   * resolved and at least one must exist in the classpath.
   * 
   * Examples:
   * 
   * categoryPrefixOrPath = new String("org.openjump.myplugin") then
   * resourcebundle is looked up as
   * /org/openjump/myplugin/language/jump[_locale].properties
   * 
   * categoryPrefixOrPath = new File("language/wfs/messages") then
   * resourcebundle is looked up as /language/wfs/messages[_locale].properties
   * 
   * @param categoryPrefixOrPath
   *          The category.
   * @return The instance.
   */
  private static I18N getInstance(final Object categoryPrefixOrPath) {
    I18N instance = instances.get(categoryPrefixOrPath);
    if (instance == null) {
      if (categoryPrefixOrPath instanceof File)
        instance = new I18N((File) categoryPrefixOrPath);
      else
        instance = new I18N(categoryPrefixOrPath.toString());
      instances.put(categoryPrefixOrPath, instance);
    }
    return instance;
  }

  /**
   * useless as there are no instance class methods. should be private, but who
   * knows what is using this code anyway.
   * 
   * use static I18N.getMessage() methods instead
   * 
   * @param categoryPrefix
   */
  public static I18N getInstance(final String categoryPrefix) {
    return getInstance((Object) categoryPrefix);
  }

  /**
   * useless as there are no instance class methods. should be private, but who
   * knows what is using this code anyway.
   * 
   * use static I18N.getMessage() methods instead
   * 
   * @param path
   */
  public static I18N getInstance(final File path) {
    return getInstance((Object) path);
  }

  /**
   * useless as there are no instance class methods. should be private, but who
   * knows what is using this code anyway.
   * 
   * use static I18N.getMessage() methods instead
   * 
   * @param path
   */
  public static I18N getInstance() {
    // is initialized statically above
    return instance;
  }

  /**
   * [ede] utility method which is used in several places
   * (loadFile,getLanguage...)
   * 
   * @param localeCode
   * @return
   */
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
   * @param loc
   */
  public static void applyToRuntime(Locale loc) {
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
    return getMessage((Object)null,label);
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
   * @param label
   *          with argument insertion : {0}
   * @param objects
   * @return i18n label
   */
  public static String getMessage(final String label, final Object... objects) {
    return getMessage((Object) null, label, objects);
  }

  /**
   * Get the internationalized text from the resource bundle associated with the
   * specified category or path. If no label is defined then a default string is
   * created from the last part of the key.
   * 
   * Examples:
   * 
   * categoryPrefixOrPath = new String("org.openjump.myplugin") then
   * resourcebundle is looked up as
   * /org/openjump/myplugin/language/jump[_locale].properties
   * 
   * categoryPrefixOrPath = new File("language/wfs/messages") then
   * resourcebundle is looked up as /language/wfs/messages[_locale].properties
   * 
   * @param categoryPrefixOrPath
   *          The category.
   * @param label
   *          Label with argument insertion : {0}
   * @param objects
   * 
   * @return i18n label
   */
  private static String getMessage(final Object categoryPrefixOrPath,
      final String label, final Object... objects) {
    I18N i18n = categoryPrefixOrPath != null ? getInstance(categoryPrefixOrPath)
        : getInstance();
    try {
      // IMPORTANT: trailing spaces break the Malayalam translation, 
      //            so we trim here, just to make sure
      String text = i18n.getText(label).trim();
      // no params, nothing to parse
      if ( objects.length < 1 )
        return text;
      // parse away
      final MessageFormat mformat = new MessageFormat(text);
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

  public static String getMessage(final String categoryPrefix,
      final String label, final Object... objects) {
    return getMessage((Object) categoryPrefix, label, objects);
  }

  public static String getMessage(final File path, final String label,
      final Object... objects) {
    return getMessage((Object) path, label, objects);
  }
}
