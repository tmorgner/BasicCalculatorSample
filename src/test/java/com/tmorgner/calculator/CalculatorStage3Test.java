package com.tmorgner.calculator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class CalculatorStage3Test {

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
      {"3.5 * 3", "10.5", ""},
      {"-53 + -24", "-77", ""},
      {"10 / 3", "3.333", ""},
      {"(-20 * 1.8) / 2", "-18", ""},
      {"-12.315 -42", "-54.315", ""},
      {"10 / 3", "3.333", ""},
      {"1 ++", "#SYNTAXERROR", ""},
      {"1 1", "#SYNTAXERROR", ""},
      {"1 / 0", "#DIV0", ""},
      {"(1 + 2 * 3", "#SYNTAXERROR", ""},
      {"6 + 3 - 2 +  ", "#SYNTAXERROR", ""},
      {"+ 1", "1", ""},
      {"2 * + 23", "46", ""},
      {"2 ^ 0.5", "1.414", "square root"},
      {"-1+2\n-3", "-2", "whitespace is ignored"},
      {"1 * (2 + (3 * 4))", "14", "nested sub-terms"},
      {"1 + 2 * 3 + 4 * 5 ^ 1", "27", "nested sub-terms"},
      {"1 * 2 - 3 * 4", "-10", "nested sub-terms"},
    });
  }

  public CalculatorStage3Test(final String input, final String result, final String justification) {

    objectsToTest = new Calculator[] {
      new CalculatorPostFix(3),
      new CalculatorSyntaxTree(3),
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
