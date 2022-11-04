package com.tmorgner.calculator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class CalculatorStage2StrictTest {

  private final Calculator[] objectsToTest;
  private final String input;
  private final String result;
  private final String justification;

  @Parameterized.Parameters(name = "{index}: Calculate(''{0}'')={1}; {2}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {"1 + 2 * 3", "7", ""},
      {"(1 + 2) * 3", "9", ""},
      {"6 + 3 - 2 + 12 ", "19", ""},
      {"2 * 15 + 23", "53", ""},
      {"10 - 3 ^ 2", "1", ""},
      {"10 - 3 ^ 2", "1", ""},
      {"1.0", "#SYNTAXERROR", "known error condition; we only accept integers for now"},
      {"+1 + -1", "#SYNTAXERROR", "known error condition; we dont accept signed numbers"},
    });
  }

  public CalculatorStage2StrictTest(final String input, final String result, final String justification) {

    objectsToTest = new Calculator[] {
      new CalculatorSyntaxTreeStage2()
    };
    this.input = input;
    this.result = result;
    this.justification = justification;
  }

  @Test
  public void Test() {
    for (final Calculator calculator : objectsToTest) {
      final String calc = calculator.calculate(input);
      Assert.assertEquals(calculator.getClass().getSimpleName() + ": " +justification, result, calc);
    }
  }

  @Before
  public void SetupGlobal() {
    LoggingSupport.INSTANCE.enable(getClass());
  }
}
