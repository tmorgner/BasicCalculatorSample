package com.tmorgner.calculator;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class BulkTest {
  @Test
  public void validateBulkData() throws IOException {
    final Calculator[] objectsToTest = new Calculator[]{
        new CalculatorPostFix(3),
        new CalculatorSyntaxTree(3),
        };

    try(final BufferedReader s = new BufferedReader(
        new InputStreamReader(getClass().getResourceAsStream("/testdata.properties"), StandardCharsets.UTF_8))) {
      String line;
      while ((line = s.readLine()) != null) {
        final String[] data = line.split(":");
        for (final Calculator calculator : objectsToTest) {
          Assert.assertEquals(data[1].trim(), calculator.calculate(data[0].trim()));
        }
      }
    }
  }
}
