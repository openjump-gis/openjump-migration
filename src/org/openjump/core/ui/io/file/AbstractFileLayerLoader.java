/* *****************************************************************************
 The Open Java Unified Mapping Platform (OpenJUMP) is an extensible, interactive
 GUI for visualizing and manipulating spatial features with geometry and
 attributes. 

 Copyright (C) 2007  Revolution Systems Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 For more information see:
 
 http://openjump.org/

 ******************************************************************************/
package org.openjump.core.ui.io.file;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.vividsolutions.jump.workbench.model.Prioritized;

/**
 * A base implementation of {@link FileLayerLoader}.
 * 
 * @author Paul Austin
 */
public abstract class AbstractFileLayerLoader implements FileLayerLoader {
  /** The descriptive name of the file format (e.g. ESRI Shapefile). */
  private String description;
  boolean unsorted = true;

  /** The list of file extensions supported by the plug-in. */
  private List<String> extensions = new ArrayList<String>();

  /** The list of Options supported by the plug-in. */
  private List<Option> optionMetadata = new ArrayList<Option>();
  
  // a comparator respecting prioritized filelayerloaders
  public static final Comparator PRIO_COMPARATOR = new Comparator<FileLayerLoader>() {
    public int compare(FileLayerLoader f1, FileLayerLoader f2) {
      int result = Prioritized.COMPARATOR.compare(f1, f2);
      if (result != 0)
        return result;

      // else sort naturally by description text
      return f1.getDescription().compareToIgnoreCase(f2.getDescription());
    }

  };

  /** default constructor for xml persistence*/
  public AbstractFileLayerLoader() {}

  /**
   * Construct a new AbstractFileLayerLoader with a description and list of
   * extensions.
   * 
   * @param description The format description.
   * @param extensions The supported file extension.
   */
  public AbstractFileLayerLoader(final String description,
    final List<String> extensions) {
    this.description = description;
    addFileExtensions( extensions );
  }

  /**
   * Get the descriptive name of the file format (e.g. ESRI Shapefile).
   * 
   * @return The file format name.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the list of file extensions supported by the plug-in.
   * 
   * @return The list of file extensions.
   */
  public Collection<String> getFileExtensions() {
    if (unsorted) {
      Collections.sort(extensions);
      unsorted = false;
    }
    return new ArrayList<String>(extensions);
  }

  /**
   * Add to the list of file extensions supported by the plug-in.
   * 
   * @param newexts list of extensions
   * @return true on success
   */
  public boolean addFileExtensions(final List<String> newexts) {
      unsorted = true;
      return extensions.addAll(newexts);
  }

  /**
   * Add an option.
   * 
   * @param name The name of the option.
   * @param type The type of the option.
   * @param required True if a value for the option is required.
   * @see Option
   */
  public void addOption(String name, String type, boolean required) {
    optionMetadata.add(new Option(name, type, required));
  }
  
  /**
   * Add an option.
   * 
   * @param name The name of the option.
   * @param type The type of the option.
   * @param defaultValue The defaultValue of the option.
   * @param required True if a value for the option is required.
   * @see Option
   */
  public void addOption(String name, String type, Object defaultValue, boolean required) {
    optionMetadata.add(new Option(name, type, defaultValue, required));
  }

  /**
   * Remove an option.
   *
   * @param name The name of the option.
   * @param type The type of the option.
   * @param required True if a value for the option is required.
   * @return true if this list contained the specified option
   * @see Option
   */
  public boolean removeOption(String name, String type, boolean required) {
	  return optionMetadata.remove(new Option(name, type, required));
  }

  /**
   * Remove an option.
   *
   * @param name The name of the option.
   * @param type The type of the option.
   * @param defaultValue The defaultValue of the option.
   * @param required True if a value for the option is required.
   * @see Option
   */
  public boolean removeOption(String name, String type, Object defaultValue, boolean required) {
    return optionMetadata.remove(new Option(name, type, defaultValue, required));
  }

  /**
   * Get the list of Options supported by the plug-in.
   * 
   * @return The list of Options.
   */
  public List<Option> getOptionMetadata() {
    return optionMetadata;
  }

  /**
   * Return a string representation of the loader.
   * 
   * @return The string.
   */
  public String toString() {
    return description;
  }

}
