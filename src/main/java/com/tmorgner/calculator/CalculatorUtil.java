package com.tmorgner.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class CalculatorUtil {
  public static final BigDecimal NEGATIVE_ONE = new BigDecimal(-1);

  public static final String LOG_PARSING_SUCCESS = "parsing success: {0}";
  public static final String LOG_PARSING_FAIL_EMPTY = "parsing failed; empty";
  public static final String LOG_PARSING_FAIL_MISSING_CLOSING_PARENTHESIS = "parsing failed; missing closing parenthesis";
  public static final String LOG_PARSING_FAIL_MISSING_OPENING_PARENTHESIS = "parsing failed; missing opening parenthesis";
  public static final String LOG_PARSING_FAIL_UNEXPECTED_SYMBOL = "parsing failed; unexpected symbol {0}";
  public static final String LOG_PARSING_FAIL_INVALID_FUNCTION = "parsing failed; unexpected function {0}";
  public static final String LOG_PARSING_MSG_SUBTERM_COMPLETE = "sub-term parsing success; {0}";
  public static final String LOG_PARSING_UNEXPECTED_ERROR = "parsing failed; unexpected error";

  public static final String LOG_EVALUATE_SUCCESS = "evaluate term ''{0}'' yields ''{1}''";
  public static final String LOG_EVALUATE_FAILED = "evaluate term ''{0}'' fails with ''{1}''";
  public static final String LOG_PARSING_FAIL_MISSING_PARAMETER = "parsing failed; missing function parameter for function {0}";

  private CalculatorUtil() {
  }

  public static BigDecimal toBigDecimal(final int scale, final double resultRaw) {
    final BigDecimal d = new BigDecimal(resultRaw).stripTrailingZeros();
    if (d.scale() > scale) {
      return d.setScale(scale, RoundingMode.HALF_UP);
    }
    else {
      return d.stripTrailingZeros();
    }
  }
}
