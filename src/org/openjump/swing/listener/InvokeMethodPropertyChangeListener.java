package org.openjump.swing.listener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingUtilities;

import org.openjump.swing.util.InvokeMethodRunnable;

public class InvokeMethodPropertyChangeListener implements
  PropertyChangeListener {
  private Runnable runnable;

  private boolean invokeLater;

  public InvokeMethodPropertyChangeListener(final Object object,
    final String methodName) {
    this(object, methodName, new Object[0]);
  }

  public InvokeMethodPropertyChangeListener(final Object object,
    final String methodName, boolean invokeLater) {
    this(object, methodName, new Object[0], false);
  }

  public InvokeMethodPropertyChangeListener(final Object object,
    final String methodName, Object[] parameters) {
    this(object, methodName, parameters, false);
  }

  public InvokeMethodPropertyChangeListener(final Object object,
    final String methodName, Object[] parameters, boolean invokeLater) {
    runnable = new InvokeMethodRunnable(object, methodName, parameters);
    this.invokeLater = invokeLater;
  }

  public void propertyChange(PropertyChangeEvent evt) {
    if (invokeLater) {
      SwingUtilities.invokeLater(runnable);
    } else {
      runnable.run();
    }
  }

}
