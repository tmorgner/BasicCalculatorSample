package com.tmorgner.calculator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class CalculatorStage5Test {

  private final Calculator[] objectsToTest;
  private final String input;
  private final String result;
  private final String justification;

  @Parameterized.Parameters(name = "{index}: Calculate(''{0}'')={1}; {2}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {"SIN(1)", "0.8414709848", ""},
      {"SIN(1,5)", "#SYNTAXERROR", ""},
      {"UNKNOWN(1)", "#SYNTAXERROR", ""},
      {"IF(1, 2, 3)", "2", ""},
      {"IF(1, RounD(Sin(5), 2), 0)", "-0.96", "Case in functions does not matter, functions can be nested"},
    });
  }

  public CalculatorStage5Test(final String input, final String result, final String justification) {

    objectsToTest = new Calculator[] {
      new CalculatorSyntaxTree()
          .declareFunction("if", Functions::If)
          .declareFunction("round", Functions::Round)
          .declareFunction("sin", Functions::Sin)
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
