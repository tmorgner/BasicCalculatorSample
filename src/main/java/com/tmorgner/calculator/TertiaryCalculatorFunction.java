package com.tmorgner.calculator;

import java.math.BigDecimal;

@FunctionalInterface
public interface TertiaryCalculatorFunction {
  BigDecimal apply(CalculatorSyntaxTree.LValue paramA,
                   CalculatorSyntaxTree.LValue paramB,
                   CalculatorSyntaxTree.LValue paramC,
                   int scale);
}
