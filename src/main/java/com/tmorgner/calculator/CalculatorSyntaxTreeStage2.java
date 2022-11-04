package com.tmorgner.calculator;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A calculator that creates a syntax tree that is a direct representation of the
 * input.
 * <p/>
 * The parsed representation of the input used in this class can be trivially mapped
 * back to a valid input string. Use this style of representation if you need to analyze
 * the input, rewrite it or provide a graphical editor.
 */
public class CalculatorSyntaxTreeStage2 implements Calculator {

  private static final Logger logger = Logger.getLogger(CalculatorSyntaxTreeStage2.class.getName());

  public CalculatorSyntaxTreeStage2() {
  }

  public String calculate(final String input) {
    if (input == null || input.trim().isEmpty()) {
      //
      return "";
    }

    final StreamTokenizer tok = new StreamTokenizer(new StringReader(input));
    tok.resetSyntax();
    tok.whitespaceChars(0, 32);
    tok.wordChars('0', '9');

    final Optional<LValue> maybeParsedTerm = parse(tok, false);
    if (!maybeParsedTerm.isPresent()) {
      return "#SYNTAXERROR";
    }

    try {
      final LValue parsedTerm = maybeParsedTerm.get();
      final String result = parsedTerm.evaluate().toPlainString();
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
  }

  Optional<LValue> parse(final StreamTokenizer tok, final boolean requireClosingParenthesis) {
    try {
      Term t = null;
      Operator op = null;

      ParseState expectation = ParseState.ExpectNumber;
      int token;
      while ((token = tok.nextToken()) != StreamTokenizer.TT_EOF) {
        if (token == StreamTokenizer.TT_WORD) {
          expectation.Ensure(ParseState.ExpectNumber);
          final Constant c = new Constant(new BigDecimal(tok.sval));
          if (t == null) {
            t = new Term(c);
          }
          else {
            t.add(op, c);
          }
          expectation = ParseState.ExpectOperator;
        }
        else if (expectation == ParseState.ExpectOperator) {
          if (token == ')') {
            expectation.Ensure(ParseState.ExpectOperator);
            logger.log(Level.FINER, CalculatorUtil.LOG_PARSING_MSG_SUBTERM_COMPLETE, t);
            return Optional.of(t);
          }

          final String str = Character.toString((char) token);
          expectation.Ensure(ParseState.ExpectOperator);
          op = Operator.TryParseOperator(str);
          expectation = ParseState.ExpectNumber;
        }
        else if (token == '(') {
          final Optional<LValue> subTerm = parse(tok, true);
          if (!subTerm.isPresent()) {
            return Optional.empty();
          }
          // any closing parenthesis must always be followed by an operator
          expectation = ParseState.ExpectOperator;
          final LValue result = subTerm.get();
          if (t == null) {
            t = new Term(result);
          }
          else {
            t.add(op, result);
          }
        }
        else {
          // Unexpected symbol
          logger.log(Level.FINER, CalculatorUtil.LOG_PARSING_FAIL_UNEXPECTED_SYMBOL, token);
          return Optional.empty();
        }
        // must be a parenthesis
      }

      if (requireClosingParenthesis) {
        logger.log(Level.FINER, CalculatorUtil.LOG_PARSING_FAIL_MISSING_CLOSING_PARENTHESIS);
        return Optional.empty();
      }

      if (t == null) {
        logger.log(Level.FINER, CalculatorUtil.LOG_PARSING_FAIL_EMPTY);
        return Optional.empty();
      }

      t.initialize();
      // make sure the parsing does not end on a trailing operator
      expectation.Ensure(ParseState.ExpectOperator);
      logger.log(Level.FINE,CalculatorUtil.LOG_PARSING_SUCCESS, t);
      return Optional.of(t);
    } catch (final IOException | ParseException ioe) {
      logger.log(Level.FINE, CalculatorUtil.LOG_PARSING_UNEXPECTED_ERROR, ioe);
      return Optional.empty();
    }
  }


  enum ParseState {
    ExpectNumber,
    ExpectOperator;

    public void Ensure(final ParseState state) throws ParseException {
      if (!this.equals(state)) {
        throw new ParseException("Expected " + this + ", but received " + state, 0);
      }
    }
  }

  interface LValue {
    BigDecimal evaluate();

    void initialize();
  }

  private static class Constant implements LValue {
    final BigDecimal value;

    public Constant(final BigDecimal value) {
      this.value = value;
    }

    @Override
    public BigDecimal evaluate() {
      return value;
    }

    @Override
    public String toString() {
      return value.toPlainString();
    }

    @Override
    public void initialize() {
    }
  }

  private static class Term implements LValue {
    private final boolean artificial;
    private final ArrayList<Operator> operators;
    private final ArrayList<LValue> terms;
    LValue head;

    public Term(final LValue head) {
      this(head, false);
    }

    public Term(final LValue head, final boolean artificial) {
      this.head = head;
      this.artificial = artificial;
      this.operators = new ArrayList<>();
      this.terms = new ArrayList<>();
    }

    public void add(final Operator op, final LValue value) {
      if (op == null) {
        throw new NullPointerException();
      }
      if (value == null) {
        throw new NullPointerException();
      }

      operators.add(op);
      terms.add(value);
    }

    public void initialize() {
      rewriteTerm();
      for (final LValue term : terms) {
        term.initialize();
      }
    }

    void rewriteTerm() {
      if (terms.size() < 2) {
        return;
      }

      int lowestOperatorLevel = operators.get(0).getPrecedence();
      boolean multipleOperators = false;
      for (final Operator op : operators) {
        if (op.getPrecedence() != lowestOperatorLevel) {
          lowestOperatorLevel = Math.min(op.getPrecedence(), lowestOperatorLevel);
          multipleOperators = true;
        }
      }

      if (!multipleOperators) {
        return;
      }

      Term subTerm = null;
      // form sub-terms for all operations that are not the lowest operator precedence
      for (int i = 0; i < terms.size(); i++) {
        final Operator op = operators.get(i);
        if (op.getPrecedence() == lowestOperatorLevel) {
          subTerm = null;
          continue;
        }

        if (subTerm == null) {
          if (i == 0) {
            subTerm = new Term(head, true);
            head = subTerm;
          }
          else {
            subTerm = new Term(terms.get(i - 1), true);
            terms.set(i - 1, subTerm);
          }
        }

        subTerm.add(op, terms.get(i));
        operators.remove(i);
        terms.remove(i);
        i -= 1;
      }
    }

    @Override
    public BigDecimal evaluate() {
      BigDecimal result = head.evaluate();
      for (int i = 0; i < operators.size(); i += 1) {
        result = operators.get(i).apply(result, terms.get(i).evaluate(), 0);
      }
      return result;
    }

    @Override
    public String toString() {
      final StringBuilder b = new StringBuilder();
      if (!artificial) {
        b.append("(");
      }
      else {
        b.append("{");
      }
      b.append(head);
      for (int i = 0; i < terms.size(); i++) {
        b.append(" ");
        b.append(operators.get(i));
        b.append(" ");
        b.append(terms.get(i));
      }
      if (!artificial) {
        b.append(")");
      }
      else {
        b.append("}");
      }
      return b.toString();
    }
  }
}
