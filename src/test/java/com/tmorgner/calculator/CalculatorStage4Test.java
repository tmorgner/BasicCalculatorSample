package com.tmorgner.calculator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class CalculatorStage4Test {

  private final Calculator[] objectsToTest;
  private final String input;
  private final String result;
  private final String justification;

  @Parameterized.Parameters(name = "{index}: Calculate(''{0}'')={1}; {2}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {null, "", ""},
      {"", "", ""},
      {" ", "", ""},
      {"1 ++", "#SYNTAXERROR", ""},
      {"1 1", "#SYNTAXERROR", ""},
      {"1 / 0", "#DIV0", ""},
      {"(1 + 2 * 3", "#SYNTAXERROR", ""},
      {"6 + 3 - 2 +  ", "#SYNTAXERROR", ""},
      {"+ 1", "1", ""},
      {"2 * + 23", "46", "JavaScript has no problem accepting signed numerals"},
      {"-1+2\n-3", "-2", "whitespace is ignored"},
      {"1 * (2 + (3 * 4))", "14", "nested sub-terms"},
      {"5 ^ 2", "#SYNTAXERROR", "The Nashorn engine does not handle the power operator (**)"},
      {"1 * 2 - 3 * 4", "-10", "nested sub-terms"},
      {"2 * 15 + 23", "53", ""},
    });
  }

  public CalculatorStage4Test(final String input, final String result, final String justification) {

    objectsToTest = new Calculator[] {
      new CalculatorScripting()
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
