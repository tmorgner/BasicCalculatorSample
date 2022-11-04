package com.tmorgner.calculator;

import java.math.BigDecimal;

@FunctionalInterface
public interface BinaryCalculatorFunction {
  BigDecimal apply(CalculatorSyntaxTree.LValue paramA,
                   CalculatorSyntaxTree.LValue paramB,
                   int scale);
}
