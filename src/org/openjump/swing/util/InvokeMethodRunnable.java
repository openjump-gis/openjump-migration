package org.openjump.swing.util;

import java.awt.event.ActionEvent;
import java.lang.reflect.Method;

public class InvokeMethodRunnable implements Runnable {

  private Object object;

  private Method method;

  private Object[] parameters;

  public InvokeMethodRunnable(final Object object, final String methodName) {
    this(object, methodName, new Object[0]);
  }

  public InvokeMethodRunnable(final Object object, final String methodName,
    final Object[] parameters) {

    this.object = object;
    this.parameters = parameters;
    Class clazz = object.getClass();
    try {
      Class[] types = new Class[parameters.length];
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals(methodName)) {
          if (method.getParameterTypes().length == parameters.length) {
            this.method = method;
          }
        }
      }
      if (method == null) {
      System.err.println(this.method + methodName);
      }
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public void run() {
    try {
      method.invoke(object, parameters);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
