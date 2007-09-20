package org.openjump.swing.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;

import org.openjump.swing.util.InvokeMethodRunnable;

/**
 * An ActionListener that invokes the method on the object when the action is
 * performed.
 * 
 * @author Paul Austin
 */
public class InvokeMethodActionListener implements ActionListener {
  private Runnable runnable;

  private boolean invokeLater;

  public InvokeMethodActionListener(final Object object, final String methodName) {
    this(object, methodName, new Object[0]);
  }
  public InvokeMethodActionListener(final Object object,
    final String methodName,  boolean invokeLater) {
    this(object, methodName, new Object[0], false);
  }

  public InvokeMethodActionListener(final Object object,
    final String methodName, Object[] parameters) {
    this(object, methodName, parameters, false);
  }

  public InvokeMethodActionListener(final Object object,
    final String methodName, Object[] parameters, boolean invokeLater) {
    runnable = new InvokeMethodRunnable(object, methodName, parameters);
    this.invokeLater = invokeLater;
  }

  public void actionPerformed(ActionEvent event) {
    if (invokeLater) {
      SwingUtilities.invokeLater(runnable);
    } else {
      runnable.run();
    }
  }

}
