package com.tmorgner.calculator;


/**
 * A basic calculator.
 * <p>
 * This class assumes that it is used in a spreadsheet compatible application. User input is handled gracefully wherever
 * possible. Expected errors (like DIV/0) any syntax errors are indicated via Error tokens (copying Excel behaviour, ie
 * #ERROR("message") for syntax errors, #DIV0 for a division by zero, etc.).
 */
public interface Calculator {
  /**
   * Evaluates the given term and returns the evaluation result or an error indicator.
   *
   * @param input
   *     an input string
   * @return the evaluated value or an error indicator starting with '#'
   */
  String calculate(String input);
}

