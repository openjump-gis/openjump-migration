package org.openjump.swing;

import javax.swing.JComponent;

import org.openjump.swing.listener.ValueChangeListener;


public interface FieldComponentFactory {
  JComponent createComponent();

  JComponent createComponent(ValueChangeListener listener);

  Object getValue(JComponent component);

  void setValue(JComponent component, Object value);
}

