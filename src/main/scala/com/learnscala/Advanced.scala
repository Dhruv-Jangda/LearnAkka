package com.learnscala

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object Advanced extends App {
  /*
      Lazy Evaluation - an expression is not evaluated until it is first used. Run below code and nothing will be printed to the console.
      Remove "lazy" from aLazyExpression to print => expression was evaluated when "lazy" is not used. Lazy evaluations are useful in
      infinite collections and other complex cases. It speeds up system due to no overhead of evaluation unless used.
     */
  lazy val aLazyValue = 2
  lazy val aLazyExpression = {
    println("Hi, I am so lazy!")
    23
  }

  /*
    Pseudo Collections: Arent actually collections - "Option". All collection methods are applicable to these objects.
   * Very useful for UNSAFE methods i.e that may return NULLs
   * Assume aFunctionWithNull is a method of large code base & can return NULL. It usually can be dealt with using IF-ELSE condition matching
      the NULL of the method's return.
   * Not required in Scala :) if we use "Option" with Pattern Matching. "Option" can be thought of a collection with at most 1 element.
   * Option(<UnsafeMethod>): Creates Some(x) if return is not NULL else None(Singleton Object, but is actually a value), "Some" is sub-type of "Option"
   */
  def aFunctionWithNull(): String = "Hi, I am Scala."
  val anOption = Option(aFunctionWithNull())
  val stringProcessing = anOption match {
    case Some(string: String) => s"Got a valid string - $string"
    case None => "Obtained Nothing"
  }

  /*
    "Try"(not try-catch): Generally used to handle UNSAFE methods that can throw exceptions. In large code bases, generally handling these functions
    within multiple try catch(sometimes nested as well) makes try-catch blocks complex and unreadable. A "Try" is a collection with value if method
    went well else an exception if method throws one. All collection functions like .map, .flatmap, .filter, etc are applicable
   */
  def aFunctionThatThrowsException(): String = throw new RuntimeException
  val aTry = Try(aFunctionThatThrowsException())
  val anotherStringProcessing = aTry match {
    case Success(validReturn) => s"Got a valid return - $validReturn"
    case Failure(exception)   => s"Got an exception - $exception"
  }

  /*
    Asynchronous Programming: Evaluating something on another thread using "Future" - another pseudo collection. Whatever is passed to Future() in
    argument is evaluated in another thread. Scala compiler expects scala.concurrent.ExecutionContext.Implicits.global to be implicitly available
    for "Future" => global is equivalent to a thread pool where threads of "Future" are evaluated. "Future" contains a value when it is evaluated.
    Being collection, can be composed with .map, .flatmap, .filter, etc.
   */
  val aFuture =
    Future({ // Future.apply(), so () can be omit for single value functions
      println("Computing a value...")
      Thread.sleep(1000)
      println("Value computed")
      65
    })

  // "Future", "Try" and "Option" are called MONADS in Scala

  /*
    Implicits - Very powerful feature of Scala compiler. Use cases:
   * Implicit arguments - Whatever defined implicit as arguments, the compiler tries to find the relevant implicit value to be readily available in
      the scope i.e. why the below method can be called without arguments, since implicit integer anImplicitInteger is already available in scope.
   * Implicit conversion - Useful in adding methods to existing types over which we don't have any control. Basically creating a wrapper around types
      to define new methods. Eg: remove "implicit" keyword in below class and .isEven() won't be available to normal integers. This makes Scala very
      expressive but at the same time very dangerous, since we're able to add/modify the base methods to/of the known types i.e. why in this case
      Implicit is used with CAUTION.
   */

  // 1. Implicit Arguments
  def aFunctionWithImplicitArgs(implicit arg: Int): Int = arg * 2
  implicit val anImplicitInteger: Int = 43
  println(
    aFunctionWithImplicitArgs
  ) // Actually the call is aFunctionWithImplicitArgs(anImplicitInteger)

  // 2. Implicit conversion
  implicit class MyRichInteger(n: Int) {
    def isEven: Boolean = n % 2 == 0
  }
  println(23.isEven) // Actually the call is - new MyRichInteger(23).isEven()


  /*
  * Synchronization(or lock) to an object that is sharable during multithreading. Imagine the class BankAccount is being used by
  * two threads subsequently:
  * 1. The "balance" variable will be accessible to both threads and they will try to override the variable
  * with their values and so will produce a final conflict.
  * */

  class BankAccount {
    private var balance = 0

    /* Every object in scala has .synchronized(<func>) method to save the variables within the block of <func> on getting updated
    when accessed by another thread. Hence a better code block when used in multithreading environment is to use .synchronized(<func>)
    */
    def deposit(amount: Int) : Unit = this.synchronized {
      if (amount > 0) balance += amount
    }

    def withdraw(amount: Int) : Unit = this.synchronized {
      if (amount > 0 && balance >= amount) balance -= amount
      else throw new Error("Insufficient Funds!")
    }
  }

  def transferFunds(from: BankAccount, to: BankAccount, amount: Int): Unit = {
    // Making sure that when "from" account is in use, it should not be updated by other thread. Same for the other one
    from.synchronized{
      to.synchronized{
        from.withdraw(amount)
        to.deposit(amount)
      }
    }
  }

  /*
  * But this synchronization is causing other issue - "Deadlocks". Eg: consequent calls to transferFunds(A,B,50) and
  * transferFunds(B,A,50) will result to Lock-on to A by "transferFunds(A,B,50)" and Lock-on to B by transferFunds(B,A,50).
  * Now further neither "B" will be accessible to "transferFunds(A,B,50)" nor "A" to transferFunds(B,A,50). Hence can't progress
  * further. One solution is to describe an order in which object locks will happen but this will make the code more complicated.
  * Hence we can define "Non-Blocking Objects"(or Akka Actors):
  * 1. Since Locking is the cause of Deadlocks.
  * 2. Also locking an object blocks a thread to access it.
  * 3. Bad CPU Utilization or slow program execution.
  * 4. Blocking also couples sender & receiver tightly i.e. the sender has to wait till the receiver is ready(or freed by another thread)
  * */
}
