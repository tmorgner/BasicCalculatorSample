package com.tmorgner.calculator;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class LoggingSupport {

  static final LoggingSupport INSTANCE;

  static {
    try(final InputStream s = CalculatorUtil.class.getResourceAsStream("/logging.properties")) {
      LogManager.getLogManager().readConfiguration(s);
    } catch (final SecurityException | IOException exception) {
      exception.printStackTrace();
    }
    INSTANCE = new LoggingSupport();
  }

  public void enable(final Class<?> c) {
    Logger.getLogger(c.getName()).setLevel(Level.FINE);
  }
}
