package com.tmorgner.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;

/**
 *
 */
public enum Operator {

  Plus("+", 0),
  Minus("-", 0),
  Multiplication("*", 1),
  Division("/", 1),
  Potency("^", 2);

  private final String token;
  private final int precedence;

  Operator(final String token, final int precedence) {
    this.token = token;
    this.precedence = precedence;
  }

  public static Operator TryParseOperator(final String input) throws ParseException {
    for (final Operator value : Operator.values()) {
      if (value.getToken().equals(input)) {
        return value;
      }
    }
    throw new ParseException("Invalid Operator " + input, 0);
  }

  public int getPrecedence() {
    return precedence;
  }

  public String getToken() {
    return token;
  }

  @Override
  public String toString() {
    return token;
  }

  public BigDecimal apply(final BigDecimal a, final BigDecimal b, final int maxScale) {
    switch (this) {
      case Plus:
        return a.add(b);
      case Minus:
        return a.subtract(b);
      case Multiplication:
        return a.multiply(b);
      case Division: {
        if (b.equals(BigDecimal.ZERO)) {
          throw new ArithmeticException("#DIV0");
        }
        return a.divide(b, maxScale, RoundingMode.HALF_UP).stripTrailingZeros();
      }
      case Potency:
        try {
          final int pot = b.intValueExact();
          return a.pow(pot);
        } catch (final ArithmeticException ignoreMe) {
          // caught if the potency is not an integer or is out of integer range
        }
        // this is a bit ugly, and may lose some precision in the extreme ranges,
        // but for our use case here this should not matter.
        final double aDouble = a.doubleValue();
        final double bDouble = b.doubleValue();
        final double result = Math.pow(aDouble, bDouble);
        return CalculatorUtil.toBigDecimal(maxScale, result);
      default:
        throw new IllegalArgumentException();
    }
  }
}
