package org.openjump.core.ui.io.file;

import java.io.File;
import java.util.Locale;

import javax.swing.filechooser.FileFilter;

public class FileNameExtensionFilter extends FileFilter {
  // Description of this filter.
  private final String description;

  // Known extensions.
  private final String[] extensions;

  // Cached ext
  private final String[] lowerCaseExtensions;

  public FileNameExtensionFilter(String description, String extension) {
    this(description, new String[] { extension});
  }

  public FileNameExtensionFilter(String description, String[] extensions) {
    if (extensions == null || extensions.length == 0) {
      throw new IllegalArgumentException(
        "Extensions must be non-null and not empty");
    }
    this.description = description;
    this.extensions = new String[extensions.length];
    this.lowerCaseExtensions = new String[extensions.length];
    for (int i = 0; i < extensions.length; i++) {
      if (extensions[i] == null || extensions[i].length() == 0) {
        throw new IllegalArgumentException(
          "Each extension must be non-null and not empty");
      }
      this.extensions[i] = extensions[i];
      lowerCaseExtensions[i] = extensions[i].toLowerCase(Locale.ENGLISH);
    }
  }

  /**
   * Tests the specified file, returning true if the file is accepted, false
   * otherwise. True is returned if the extension matches one of the file name
   * extensions of this {@code FileFilter}, or the file is a directory.
   * 
   * @param f the {@code File} to test
   * @return true if the file is to be accepted, false otherwise
   */
  public boolean accept(File f) {
    if (f != null) {
      if (f.isDirectory()) {
        return true;
      }
      // NOTE: we tested implementations using Maps, binary search
      // on a sorted list and this implementation. All implementations
      // provided roughly the same speed, most likely because of
      // overhead associated with java.io.File. Therefor we've stuck
      // with the simple lightweight approach.
      String fileName = f.getName();
      int i = fileName.lastIndexOf('.');
      if (i > 0 && i < fileName.length() - 1) {
        String desiredExtension = fileName.substring(i + 1).toLowerCase(
          Locale.ENGLISH);
        for (String extension : lowerCaseExtensions) {
          if (desiredExtension.equals(extension)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * The description of this filter. For example: "JPG and GIF Images."
   * 
   * @return the description of this filter
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns the set of file name extensions files are tested against.
   * 
   * @return the set of file name extensions files are tested against
   */
  public String[] getExtensions() {
    String[] result = new String[extensions.length];
    System.arraycopy(extensions, 0, result, 0, extensions.length);
    return result;
  }

  /**
   * Returns a string representation of the {@code FileNameExtensionFilter}.
   * This method is intended to be used for debugging purposes, and the content
   * and format of the returned string may vary between implementations.
   * 
   * @return a string representation of this {@code FileNameExtensionFilter}
   */
  public String toString() {
    return super.toString() + "[description=" + getDescription()
      + " extensions=" + java.util.Arrays.asList(getExtensions()) + "]";
  }
}
