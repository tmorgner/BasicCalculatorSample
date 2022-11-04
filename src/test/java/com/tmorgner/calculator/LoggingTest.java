package com.tmorgner.calculator;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class LoggingTest {
  @BeforeClass
  public static void SetupGlobal() {
    // Java's printf syntax is a very specific kind of evil
    // ... as are the silent config failures of the JDK's own log framework
    try(final InputStream s = CalculatorUtil.class.getResourceAsStream("/logging.properties")) {
      LogManager.getLogManager().readConfiguration(s);
    } catch (final SecurityException | IOException exception) {
      exception.printStackTrace();
    }
  }

  @Test
  public void Test(){
    final Logger logger = Logger.getLogger(LoggingTest.class.getName());
    logger.log(Level.INFO, "Info");
    logger.log(Level.SEVERE, "Severe");
    logger.log(Level.FINE, "Fine");
  }
}
