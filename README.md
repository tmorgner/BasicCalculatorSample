# Calculators

This repository contains three implementations of a basic calculator function.
Each function takes a string and returns its result as string.

All implementations use different internal representations of the processed input
to enable a discussion about the various trade-offs inherent in the designs.

* [CalculatorSyntaxTree](src/main/java/com/tmorgner/calculator/CalculatorSyntaxTree.java) A calculator using an SyntaxTree
  
  This implementation produces a syntax tree that preserves the original structure 
  of the input. It has slightly higher memory use than the postfix version.

* [CalculatorPostfix](src/main/java/com/tmorgner/calculator/CalculatorPostFix.java) A calculator using postfix notation

  Postfix notation sacrifices the inputs original structure for runtime performance. 
  Evaluation of the parsed result is trivial and particularly cache friendly.

* [CalculatorScripting](src/main/java/com/tmorgner/calculator/CalculatorScripting.java) A calculator using postfix notation

  The fastest way to an implementation is the implementation you don't have to 
  write yourself. This calculator trades control over the execution against 
  performance and developer time.