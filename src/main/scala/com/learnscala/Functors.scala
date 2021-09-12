package com.learnscala

import scala.util.{Success, Try}

object Functors extends App {
  val aTry: Try[Int] = Success(42)
  val anOption: Option[Int] = Some(10)

  val aTransformedTry: Try[Int] = aTry.map(_ * 10) // returns Success(420)
  val aTransformedOption: Option[Int] = anOption.map(_ + 10) // returns Some(20)

  /*
    Since above .map is applicable to multiple collections(or .map is TRANSFERABLE) we can define a general method to perform common
    operations - FUNCTOR. Can be defined using type class.
   */

  // Eg: Here, we're defining different function for each type but all doing the same thing
  def do10xList(listOfInts: List[Int]): List[Int] = listOfInts.map(_ * 10)
  def do10xOpt(listOfOpts: Option[Int]): Option[Int] = listOfOpts.map(_ * 10)
  def do10xTry(listOfTrys: Try[Int]): Try[Int] = listOfTrys.map(_ * 10)

  /*
    In Scala, a TRANSFERABLE concept can be easily described by the type CLASS. Below trait Functor takes a generic 'C'(~collection) of
    a generic '_'(~Int, String, etc).
   * Defining the transferable method ".map" that defines types 'A', 'B'(~Int, String, etc) to be used in generic collection variable <container>.
   This is how HOFs are defined.
   * Argument 1 - a generic container C[A](~collection[Int/String])
   * Argument 2 - a function defined(in same fashion as 'FunctionX') from type 'A'(type of input) to 'B'(type of output)
   * Output - of type container C[B](~collection[Int/String])
   */
  trait Functor[C[_]] {
    def map[A,B](container: C[A])(f: A => B) : C[B]
  }

  /*
   * Extending Functor to List. Below code is valid on Scala 3, due to "given" keyword. In Scala 3 "given" is used with "using"
   given listFunctor as Functor[List] {
    override def map[A,B](container: List[A])(f: A => B) : C[B] = container.map(f)
   }
    */

  /*
  In order to use the concept .map created above, the Functor has to be implemented as per the data structure we want to use .map on

  * Below code is valid in Scala 3, due to "using" keyword
  def do10x[C[_]](container: C[Int])(using functor: Functor[C]): C[Int] =
    functor.map(C[Int])(_ * 10)

  * Calling below are equivalent, hence a Functor provides a way to define a "stable API" to be used with any type of data structure
  do10xList(List(1,2,3))
  do10x(List(1,2,3)) <- Works only when Functor[List] is in scope, since listFunctor is passed implicitly
    */

  // Ref: https://www.youtube.com/watch?v=aSnY2JBzjUw
}
