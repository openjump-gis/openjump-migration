package org.openjump.core.ui.io.file;

import java.util.Collection;
import java.util.Iterator;

public class FileLayerLoaderExtensionFilter extends FileNameExtensionFilter {

  private FileLayerLoader fileLayerLoader;

  public FileLayerLoaderExtensionFilter(FileLayerLoader fileLayerLoader) {
    super(getDescription(fileLayerLoader), getExtensionArray(fileLayerLoader));
    this.fileLayerLoader = fileLayerLoader;
  }

  public FileLayerLoader getFileLoader() {
    return fileLayerLoader;
  }

  private static String createDescription(final String description,
    Collection fileExtensions) {
    StringBuffer fullDescription = new StringBuffer(description);
    fullDescription.append(" (");
    for (Iterator extensions = fileExtensions.iterator(); extensions.hasNext();) {
      String extension = (String)extensions.next();
      fullDescription.append("*.");
      fullDescription.append(extension);
      if (extensions.hasNext()) {
        fullDescription.append(",");
      }
    }
    fullDescription.append(" )");
    return fullDescription.toString();
  }

  private static String getDescription(FileLayerLoader fileLayerLoader) {
    return createDescription(fileLayerLoader.getDescription(),
      fileLayerLoader.getFileExtensions());
  }

  private static String[] getExtensionArray(FileLayerLoader fileLayerLoader) {
    return fileLayerLoader.getFileExtensions().toArray(new String[0]);
  }

}
