package org.openjump.swing.listener;

import java.util.EventObject;

public class ValueChangeEvent extends EventObject {

  private Object value;

  public ValueChangeEvent(Object source, Object value) {
    super(source);
    this.value = value;
  }

  /**
   * @return the value
   */
  public Object getValue() {
    return value;
  }

}
