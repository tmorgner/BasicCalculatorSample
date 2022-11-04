package com.tmorgner.calculator;

import java.math.BigDecimal;

@FunctionalInterface
public interface UnaryCalculatorFunction {
  BigDecimal apply(CalculatorSyntaxTree.LValue param, int scale);
}
