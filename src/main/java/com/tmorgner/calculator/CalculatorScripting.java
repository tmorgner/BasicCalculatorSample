package com.tmorgner.calculator;

import javax.script.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A calculator that trades implementation velocity against the ability to introduce new operators.
 * <p/>
 * If all we need is fast math evaluation, and we trust the source of the data, then it might be
 * more efficient to reuse already existing languages. However, for untrusted data, this implementation
 * can be dangerous as it allows arbitrary script evaluation.
 */
public class CalculatorScripting implements Calculator {
  private static final Logger logger = Logger.getLogger(CalculatorScripting.class.getName());
  private final ScriptEngine engine;

  public CalculatorScripting() {
    final ScriptEngineManager manager = new ScriptEngineManager();
    engine = manager.getEngineByMimeType("application/javascript");
    if (engine == null) {
      throw new IllegalStateException("This JDK does not support 'javascript' scripting.");
    }
  }

  @Override
  public String calculate(final String input) {

    if (input == null || input.trim().isEmpty()) {
      //
      return "";
    }

    if (input.contains("^")) {
      // The javascript operator ^ is not a power operator and the old code in the NashornEngine does
      // not support the modern power operator **
      final String error = "#SYNTAXERROR";
      logger.log(Level.FINE, CalculatorUtil.LOG_EVALUATE_FAILED, new Object[]{input, error});
      return error;
    }

    try {
      final Object o;
      if (engine instanceof Compilable) {
        final Compilable c = (Compilable) engine;
        final CompiledScript s = c.compile(input);
        o = s.eval();
      }
      else {
        o = engine.eval(input);
      }

      if (o == null) {
        return "";
      }
      if (o instanceof Double) {
        final Double d = (Double) o;
        if (d.isInfinite()) {
          final String error = "#DIV0";
          logger.log(Level.FINE, CalculatorUtil.LOG_EVALUATE_FAILED, new Object[]{input, error});
          return error;
        }
      }

      final String result = String.valueOf(o);
      logger.log(Level.FINE, CalculatorUtil.LOG_EVALUATE_SUCCESS, new Object[]{input, result});
      return result;
    } catch (final ScriptException e) {
      final String error = "#SYNTAXERROR";
      logger.log(Level.FINE, CalculatorUtil.LOG_EVALUATE_FAILED, new Object[]{input, error});
      return error;
    }
  }
}
