package com.tmorgner.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.tmorgner.calculator.CalculatorUtil.toBigDecimal;

public final class Functions {
  private Functions() {
  }

  public static BigDecimal Sin(final CalculatorSyntaxTree.LValue b, final int scale) {
    final BigDecimal v = b.evaluate(scale);
    final double resultRaw = Math.sin(v.doubleValue());
    return toBigDecimal(scale, resultRaw);
  }

  public static BigDecimal If(final CalculatorSyntaxTree.LValue condition,
                              final CalculatorSyntaxTree.LValue whenNotZero,
                              final CalculatorSyntaxTree.LValue whenZero, final int scale) {
    final BigDecimal v = condition.evaluate(scale);
    if (BigDecimal.ZERO.equals(v)) {
      return whenZero.evaluate(scale);
    }
    else {
      return whenNotZero.evaluate(scale);
    }
  }

  public static BigDecimal Round(final CalculatorSyntaxTree.LValue value,
                                 final CalculatorSyntaxTree.LValue precision, final int scale) {
    final BigDecimal v = value.evaluate(scale);
    final BigDecimal p = precision.evaluate(0);
    return v.setScale(p.intValueExact(), RoundingMode.HALF_UP);
  }

}
