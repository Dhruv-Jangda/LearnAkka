package com.learnscala

object Variables extends App {
  // In scala, we think in terms of expressions, since everything is an expression
  val anExpression: Int = 4 + 5

  // String extrapolation
  val aSimpleString: String = "Dhruv"
  val extrapolatedString: String = s"My name is $aSimpleString"

  // If-expression. Can be extended via chained if-else
  val ifExpression: Int = if (anExpression > 5) 10 else -10
  val chainedIfExpression: Int =
    if (anExpression > 10) 20
    else if (anExpression < 3) -10
    else if (anExpression < 8) -20
    else 0

  // An example of code block. A code block is also the body of a function
  val aCodeBlock: Int = {
    var aDummyVariable: Int = 5
    // Any functions or expressions, but should have the last line i.e the value assigned to x
    aDummyVariable += 1
    aDummyVariable ^ 4
  }

  /*
    return type "Unit". Actually return type of SIDE EFFECTS. It is a key term in Functional Programming, which means
    anything that does no computation: Eg. storing a value, printing to console. Anything that returns {} is type Unit.
   */
  val unitReturn = {}

  // In Scala, looping is heavily discouraged due to its functional behaviour. Hence it should be thought always in terms of RECURSION
  def factorial(n: Int): Int =
    if (n <= 1) 1
    else n * factorial(n - 1)
  /* Recursion is evaluated in Stacks.Eg:
    factorial(5) = 5 * factorial(4)
    factorial(4) = 4 * factorial(3)
    factorial(3) = 3 * factorial(2)
    factorial(2) = 2 * factorial(1)
    factorial(1) = 1
    Stacking(LIFO) as [factorial(5), factorial(4), factorial(3), factorial(2), factorial(1), 1]
    On evaluation/computation unstacking happens i.e. factorial(1) unstacked then replaced by its value, then factorial(2) and so on.
   */
  val factorialNumber: Int = factorial(5)
}
