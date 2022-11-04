package com.tmorgner.calculator;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A calculator that uses postfix notation to optimize term evaluation performance.
 * <p/>
 * This calculator trades the ability to preserve the input's structure for slightly better
 * execution time.
 */
public class CalculatorPostFix implements Calculator {

  private int scale;

  private static final Logger logger = Logger.getLogger(CalculatorPostFix.class.getName());

  public CalculatorPostFix() {
    this.scale = 10;
  }

  public CalculatorPostFix(final int scale) {
    this.scale = scale;
  }

  public int getScale() {
    return scale;
  }

  public void setScale(final int scale) {
    this.scale = scale;
  }

  public CalculatorPostFix withScale(final int scale) {
    this.scale = scale;
    return this;
  }

  @Override
  public String calculate(final String input) {
    if (input == null || input.trim().isEmpty()) {
      //
      return "";
    }

    final Optional<ArrayList<Object>> maybeExpression = parse(input);
    if (!maybeExpression.isPresent()) {
      return "#SYNTAXERROR";
    }

    try {
      final String result = evaluateExpression(maybeExpression.get());
      logger.log(Level.FINE, CalculatorUtil.LOG_EVALUATE_SUCCESS, new Object[]{input, result});
      return result;
    } catch (final ArithmeticException e) {
      final String error;
      if (e.getMessage().startsWith("#")) {
        error = e.getMessage();
      }
      else {
        error = "#ERROR(" + e.getMessage() + ")";
      }
      logger.log(Level.FINE, CalculatorUtil.LOG_EVALUATE_FAILED, new Object[]{input, error});
      return error;
    }
    catch (final EmptyStackException e) {
      final String error = "#SYNTAXERROR";
      logger.log(Level.FINE, CalculatorUtil.LOG_EVALUATE_FAILED, new Object[]{input, error});
      return error;
    }
  }

  private String evaluateExpression(final ArrayList<Object> expression) {
    final Stack<BigDecimal> evalStack = new Stack<>();
    for (final Object x : expression) {
      if (x instanceof BigDecimal) {
        evalStack.add((BigDecimal) x);
      }

      if (x instanceof Operator) {
        final Operator op = (Operator) x;

        final BigDecimal op2 = evalStack.pop();
        final BigDecimal op1 = evalStack.pop();
        evalStack.push(op.apply(op1, op2, scale));
      }
    }
    return evalStack.pop().toPlainString();
  }

  private static StreamTokenizer createTokenizer(final String input) {
    final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    final StreamTokenizer tok = new StreamTokenizer(new StringReader(input));
    tok.resetSyntax();
    tok.whitespaceChars(0, 32);
    tok.wordChars('0', '9');
    tok.wordChars(symbols.getDecimalSeparator(), symbols.getDecimalSeparator());
    return tok;
  }

  private Optional<ArrayList<Object>> parse(final String input) {
    final StreamTokenizer tok = createTokenizer(input);
    final Stack<Object> operatorStack = new Stack<>();
    final ArrayList<Object> expression = new ArrayList<>();
    try {
      BigDecimal sign = BigDecimal.ONE;
      ParseState parseState = ParseState.ExpectNumber;
      int token;
      while ((token = tok.nextToken()) != StreamTokenizer.TT_EOF) {
        if (token == StreamTokenizer.TT_WORD) {
          parseState.Ensure(ParseState.ExpectNumber);
          parseState = ParseState.ExpectOperator;
          expression.add(new BigDecimal(tok.sval).multiply(sign));
          sign = BigDecimal.ONE;
          continue;
        }

        if (parseState == ParseState.ExpectNumber) {
          // must be an opening parenthesis
          // we'll also allow leading signs for the first operand.
          // (it just feels more complete this way)
          if (token == '(') {
            operatorStack.push('(');
          }
          else if (token == '+' || token == '-') {
            sign = token == '-' ? CalculatorUtil.NEGATIVE_ONE : BigDecimal.ONE;
          }
          else {
            logger.log(Level.FINER, CalculatorUtil.LOG_PARSING_FAIL_UNEXPECTED_SYMBOL, token);
            return Optional.empty();
          }
        }

        if (parseState == ParseState.ExpectOperator) {
          if (token == ')') {
            // either operator or closing parenthesis
            if (!unwindOperatorStack(expression, operatorStack)) {
              logger.log(Level.FINER, CalculatorUtil.LOG_PARSING_FAIL_MISSING_CLOSING_PARENTHESIS);
              return Optional.empty();
            }
          }
          else {
            // must be an operator
            final String str = Character.toString((char) token);
            parseState.Ensure(ParseState.ExpectOperator);
            parseState = ParseState.ExpectNumber;
            final Operator op = Operator.TryParseOperator(str);

            unwindHigherPrecedenceOperators(expression, operatorStack, op);
            operatorStack.push(op);
          }
        }
      }

      if (unwindOperatorStack(expression, operatorStack)) {
        logger.log(Level.FINER, CalculatorUtil.LOG_PARSING_FAIL_MISSING_CLOSING_PARENTHESIS);
        return Optional.empty();
      }

      if (expression.isEmpty()) {
        logger.log(Level.FINER, CalculatorUtil.LOG_PARSING_FAIL_EMPTY);
        return Optional.empty();
      }

      logger.log(Level.FINE, CalculatorUtil.LOG_PARSING_SUCCESS, expression);
      return Optional.of(expression);
    } catch (final IOException | NumberFormatException | ParseException ioe) {
      logger.log(Level.FINE, CalculatorUtil.LOG_PARSING_UNEXPECTED_ERROR, ioe);
      return Optional.empty();
    }
  }

  private void unwindHigherPrecedenceOperators(final ArrayList<Object> expression,
                                               final Stack<Object> operatorStack,
                                               final Operator op) {
    while (!operatorStack.isEmpty()) {
      final Object maybeParenthesis = operatorStack.peek();
      if (maybeParenthesis == Character.valueOf('(')) {
        return;
      }
      final Operator existingOp = (Operator) maybeParenthesis;
      if (existingOp.getPrecedence() >= op.getPrecedence()) {
        expression.add(existingOp);
        operatorStack.pop();
      }
      else {
        return;
      }
    }
  }

  private boolean unwindOperatorStack(final ArrayList<Object> expression,
                                      final Stack<Object> operatorStack) {
    while (!operatorStack.isEmpty()) {
      final Object maybeParenthesis = operatorStack.pop();
      if (maybeParenthesis == Character.valueOf('(')) {
        return true;
      }
      expression.add(maybeParenthesis);
    }
    return false;
  }

  private enum ParseState {
    ExpectNumber,
    ExpectOperator;

    public void Ensure(final ParseState state) throws ParseException {
      if (!this.equals(state)) {
        throw new ParseException("Expected " + state + ", but received " + this, 0);
      }
    }
  }
}
