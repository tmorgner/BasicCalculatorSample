package com.tmorgner.calculator;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A calculator that creates a syntax tree that is a direct representation of the input.
 * <p/>
 * The parsed representation of the input used in this class can be trivially mapped back to a valid input string. Use
 * this style of representation if you need to analyze the input, rewrite it or provide a graphical editor.
 */
public class CalculatorSyntaxTree implements Calculator {

  private static class FunctionDeclaration {
    private final String name;
    private final int parameterCount;
    private final Object function;

    public FunctionDeclaration(final String name, final UnaryCalculatorFunction fn) {
      this.name = name;
      this.parameterCount = 1;
      this.function = fn;
    }

    public FunctionDeclaration(final String name, final BinaryCalculatorFunction fn) {
      this.name = name;
      this.parameterCount = 2;
      this.function = fn;
    }

    public FunctionDeclaration(final String name, final TertiaryCalculatorFunction fn) {
      this.name = name;
      this.parameterCount = 3;
      this.function = fn;
    }

    public String getName() {
      return name;
    }

    public int getParameterCount() {
      return parameterCount;
    }

    public <T> T getFunctor(final Class<T> t) throws ParseException {
      if (t.isInstance(function)) {
        return (T) function;
      }
      throw new ParseException("Function type mismatch", 0);
    }
  }

  private static final Logger logger = Logger.getLogger(CalculatorSyntaxTree.class.getName());
  private int scale;
  private final HashMap<String, FunctionDeclaration> functions;

  public CalculatorSyntaxTree() {
    this(10);
  }

  public CalculatorSyntaxTree(final int scale) {
    this.scale = scale;
    this.functions = new HashMap<>();
  }

  public CalculatorSyntaxTree declareFunction(final String name, final UnaryCalculatorFunction fn) {
    this.functions.put(name, new FunctionDeclaration(name, fn));
    return this;
  }

  public CalculatorSyntaxTree declareFunction(final String name, final BinaryCalculatorFunction fn) {
    this.functions.put(name, new FunctionDeclaration(name, fn));
    return this;
  }

  public CalculatorSyntaxTree declareFunction(final String name, final TertiaryCalculatorFunction fn) {
    this.functions.put(name, new FunctionDeclaration(name, fn));
    return this;
  }

  public int getScale() {
    return scale;
  }

  public void setScale(final int scale) {
    this.scale = scale;
  }

  public CalculatorSyntaxTree withScale(final int scale) {
    this.scale = scale;
    return this;
  }

  public String calculate(final String input) {
    if (input == null || input.trim().isEmpty()) {
      //
      return "";
    }

    final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    final StreamTokenizer tok = new StreamTokenizer(new StringReader(input));
    tok.resetSyntax();
    tok.whitespaceChars(0, 32);
    tok.wordChars('0', '9');
    tok.wordChars(symbols.getDecimalSeparator(), symbols.getDecimalSeparator());
    tok.wordChars('a', 'z');
    tok.wordChars('A', 'Z');

    final Optional<LValue> maybeParsedTerm = parse(tok, false);
    if (!maybeParsedTerm.isPresent()) {
      return "#SYNTAXERROR";
    }

    try {
      final LValue parsedTerm = maybeParsedTerm.get();
      final String result = parsedTerm.evaluate(scale).toPlainString();
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
      BigDecimal sign = BigDecimal.ONE;
      while ((token = tok.nextToken()) != StreamTokenizer.TT_EOF) {
        if (token == StreamTokenizer.TT_WORD) {
          expectation.Ensure(ParseState.ExpectNumber);
          expectation = ParseState.ExpectOperator;
          final Optional<LValue> maybeLValue = parseLValue(tok, sign);
          if (!maybeLValue.isPresent()) {
            return Optional.empty();
          }
          sign = BigDecimal.ONE;
          if (t == null) {
            t = new Term(maybeLValue.get());
          }
          else {
            t.add(op, maybeLValue.get());
          }
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
        else if (token == '-' || token == '+') {
          sign = token == '-' ? CalculatorUtil.NEGATIVE_ONE : BigDecimal.ONE;
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
      logger.log(Level.FINE, CalculatorUtil.LOG_PARSING_SUCCESS, t);
      return Optional.of(t);
    } catch (final IOException | ParseException ioe) {
      logger.log(Level.FINE, CalculatorUtil.LOG_PARSING_UNEXPECTED_ERROR, ioe);
      return Optional.empty();
    }
  }


  Optional<LValue> parseLValue(final StreamTokenizer tok) throws IOException, ParseException {
    BigDecimal sign = BigDecimal.ONE;
    tok.nextToken();

    if (tok.ttype == '+') {
      tok.nextToken();
    }
    else if (tok.ttype == '-') {
      tok.nextToken();
      sign = CalculatorUtil.NEGATIVE_ONE;
    }
    return parseLValue(tok, sign);
  }

  Optional<LValue> parseLValue(final StreamTokenizer tok, final BigDecimal sign) throws IOException, ParseException {
    if (tok.sval == null) {
      throw new ParseException("Unexpected error", 0);
    }
    final FunctionDeclaration maybeFunction = functions.get(tok.sval.toLowerCase(Locale.US));
    final LValue lValue;
    if (maybeFunction != null) {
      if (tok.nextToken() != '(') {
        logger.log(Level.FINER, CalculatorUtil.LOG_PARSING_FAIL_MISSING_OPENING_PARENTHESIS, maybeFunction.getName());
        return Optional.empty();
      }
      final Optional<LValue> fn = parseFunction(tok, maybeFunction);
      if (!fn.isPresent()) {
        return Optional.empty();
      }

      if (BigDecimal.ONE.equals(sign)) {
        lValue = fn.get();
      }
      else {
        // To totally preserve the structure of the input, introduce a prefix operator
        final Term t = new Term(new Constant(sign));
        t.add(Operator.Multiplication, fn.get());
        lValue = t;
      }
    }
    else {
      try {
        lValue = new Constant(new BigDecimal(tok.sval).multiply(sign));
      } catch (final NumberFormatException nf) {
        logger.log(Level.FINE, CalculatorUtil.LOG_PARSING_FAIL_INVALID_FUNCTION, tok.sval);
        return Optional.empty();
      }
    }
    return Optional.of(lValue);
  }

  Optional<LValue> parseFunction(final StreamTokenizer tok, final FunctionDeclaration fn) throws IOException, ParseException {
    if (fn.getParameterCount() == 1) {
      final Optional<LValue> param = parseLValue(tok);
      final int nx = tok.nextToken();
      if (nx != ')') {
        logger.log(Level.FINER, CalculatorUtil.LOG_PARSING_FAIL_MISSING_CLOSING_PARENTHESIS, fn.getName());
        return Optional.empty();
      }
      if (param.isPresent()) {
        final UnaryCalculatorFunction c = fn.getFunctor(UnaryCalculatorFunction.class);
        return Optional.of(new UnaryFunction(fn.getName(), c, param.get()));
      }
    }
    else if (fn.getParameterCount() == 2) {
      final Optional<LValue> paramA = parseLValue(tok);
      if (tok.nextToken() != ',') {
        return Optional.empty();
      }
      final Optional<LValue> paramB = parseLValue(tok);
      if (tok.nextToken() != ')') {
        logger.log(Level.FINER, CalculatorUtil.LOG_PARSING_FAIL_MISSING_CLOSING_PARENTHESIS, fn.getName());
        return Optional.empty();
      }
      if (paramA.isPresent() && paramB.isPresent()) {
        return Optional.of(new BinaryFunction(fn.getName(),
                                              fn.getFunctor(BinaryCalculatorFunction.class),
                                              paramA.get(),
                                              paramB.get()));
      }

    }
    else if (fn.getParameterCount() == 3) {
      final Optional<LValue> paramA = parseLValue(tok);
      if (tok.nextToken() != ',') {
        return Optional.empty();
      }
      final Optional<LValue> paramB = parseLValue(tok);
      if (tok.nextToken() != ',') {
        logger.log(Level.FINER, CalculatorUtil.LOG_PARSING_FAIL_MISSING_PARAMETER, fn.getName());
        return Optional.empty();
      }
      final Optional<LValue> paramC = parseLValue(tok);
      if (tok.nextToken() != ')') {
        logger.log(Level.FINER, CalculatorUtil.LOG_PARSING_FAIL_MISSING_CLOSING_PARENTHESIS, fn.getName());
        return Optional.empty();
      }
      if (paramA.isPresent() && paramB.isPresent() && paramC.isPresent()) {
        return Optional.of(new TertiaryFunction(fn.getName(),
                                                fn.getFunctor(TertiaryCalculatorFunction.class),
                                                paramA.get(),
                                                paramB.get(),
                                                paramC.get()));
      }

    }
    return Optional.empty();
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
    BigDecimal evaluate(int scale);

    void initialize();
  }

  private static class Constant implements LValue {
    final BigDecimal value;

    public Constant(final BigDecimal value) {
      this.value = value;
    }

    @Override
    public BigDecimal evaluate(final int scale) {
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
    public BigDecimal evaluate(final int scale) {
      BigDecimal result = head.evaluate(scale);
      for (int i = 0; i < operators.size(); i += 1) {
        result = operators.get(i).apply(result, terms.get(i).evaluate(scale), scale);
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

  public static class UnaryFunction implements LValue {
    private final String name;
    private final UnaryCalculatorFunction fn;
    private final LValue param;

    public UnaryFunction(final String name,
                         final UnaryCalculatorFunction fn,
                         final LValue param) {
      this.name = name;
      this.fn = fn;
      this.param = param;
    }

    @Override
    public BigDecimal evaluate(final int scale) {
      return fn.apply(param, scale);
    }

    @Override
    public void initialize() {
      param.initialize();
    }

    @Override
    public String toString() {
      return name + "(" + param + ")";
    }
  }

  public static class BinaryFunction implements LValue {
    private final String name;
    private final BinaryCalculatorFunction fn;
    private final LValue paramA;
    private final LValue paramB;

    public BinaryFunction(final String name,
                          final BinaryCalculatorFunction fn,
                          final LValue paramA,
                          final LValue paramB) {
      this.name = name;
      this.fn = fn;
      this.paramA = paramA;
      this.paramB = paramB;
    }

    @Override
    public BigDecimal evaluate(final int scale) {
      return fn.apply(paramA, paramB, scale);
    }

    @Override
    public void initialize() {
      paramA.initialize();
      paramB.initialize();
    }

    @Override
    public String toString() {
      return name + "(" + paramA + ", " + paramB + ")";
    }
  }

  public static class TertiaryFunction implements LValue {
    private final String name;
    private final TertiaryCalculatorFunction fn;
    private final LValue paramA;
    private final LValue paramB;
    private final LValue paramC;

    public TertiaryFunction(final String name,
                            final TertiaryCalculatorFunction fn,
                            final LValue paramA,
                            final LValue paramB,
                            final LValue paramC) {
      this.name = name;
      this.fn = fn;
      this.paramA = paramA;
      this.paramB = paramB;
      this.paramC = paramC;
    }

    @Override
    public BigDecimal evaluate(final int scale) {
      return fn.apply(paramA, paramB, paramC, scale);
    }

    @Override
    public void initialize() {
      paramA.initialize();
      paramB.initialize();
    }

    @Override
    public String toString() {
      return name + "(" + paramA + ", " + paramB + ", " + paramC + ")";
    }
  }
}
