package com.tmorgner.calculator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class CalculatorStage1Test {

  private final Calculator[] objectsToTest;

  private final String input;
  private final String result;
  private final String justification;

  @Parameterized.Parameters(name = "{index}: Calculate(''{0}'')={1}; {2}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {"11", "11", ""},
      {"1 + 1", "2", ""},
      {"2 + 5", "7", ""},
      {"8 - 3", "5", ""},
      {"5 * 4", "20", ""},
      {"8 / 2", "4", ""},
      {"4 ^ 2", "16", ""},
      {"1", "1", "single term expression are still somewhat valid"},
      {"1/0", "#DIV0", "known error condition"},
      {"1 /", "#SYNTAXERROR", "known error condition"},
    });
  }

  public CalculatorStage1Test(final String input, final String result, final String justification) {

    objectsToTest = new Calculator[] {
      new CalculatorPostFix(),
      new CalculatorSyntaxTree(),
    };
    this.input = input;
    this.result = result;
    this.justification = justification;
  }

  @Test
  public void Test() {
    for (final Calculator calculator : objectsToTest) {
      final String calculate = calculator.calculate(input);
      Assert.assertEquals(calculator.getClass().getSimpleName() + ": " +justification, result, calculate);
    }
  }

  @Before
  public void SetupGlobal() {
    LoggingSupport.INSTANCE.enable(getClass());
  }
}
