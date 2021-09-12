package com.learnscala

import scala.reflect.runtime.universe.TypeTag

object TypeLevelProgramming extends App {

  // Boilerplate
    def show[T](value: T)(implicit tag: TypeTag[T]) = tag.toString().replace("com.learnscala.TypeLevelProgramming.", "")
    print(show(List(1,2,3)))

  /*
  * Type-level programming vs Normal programming
  * Normal programming works with normal or known type of values i.e. int,char,float, etc with each allocated memory. So
  * normal programming happens at run-time after code gets compiled. Type-level programming involves compiler as a program
  * executor i.e. compiler will infer some type constraints, some relation between types that will be response a mathematical
  * problem or to other process. So it is programming by enforcing some type constraints and compiler validates those types
  * at compile time only i.e. before run time. In normal programming the type is already known hence run-time of that part
  * will be short as compared to one with Type-level programming
  */

  // Example: Type to represent natural numbers. (without using int type). Peano representation
  trait Nat
  class _0 extends Nat
  class Succ[T <: Nat] extends Nat // type "T" should be child of "Nat"

  // "type" is used to define a new type like int,char,float,etc
  type _1 = Succ[_0]
  type _2 = Succ[_1] // Succ[Succ[_0]]
  type _3 = Succ[_2]
  // type newInt = Succ[Int] - will throw an error that "Int" is not a child of "Nat"

  // Though we can say that _2 < _3, since _3 is Succ[_2], but to compare _2 and _4
  trait <[A <: Nat, B <: Nat]
  object < {

    /*
    * Will return an object of type <[0, Succ[B]] automatically, hence a variable of type <[_0, _1], <[_0, _2], etc can
    * be defined but not <[_1, _2]. Hence to make this comparison true, object of such type has to be readily available.
    * */
    implicit def lessThanBasic[B <: Nat]: <[_0, Succ[B]] = new <[_0, Succ[B]] {}
    implicit def inductive[A <: Nat, B <: Nat](implicit lt: <[A,B]) : <[Succ[A], Succ[B]] = new <[Succ[A], Succ[B]] {}
    def apply[A <: Nat, B <: Nat](implicit lessThan : <[A,B]): <[A,B] = lessThan
    /*
    * Syntactic Sugar - as a type(& not as object) <[A,B] can be written as A < B. Only due to this implicit in .apply
    * we are able to define
    * */
  }

  val comparison: _1 < _3 = <[_1, _3]
  // val invalidComp: _3 < _1 = <[_3,_1] This won't compile. Error - No implicits found for <[_1,_3]
  /*
  * Initializing an object of type <[_1,_3] calls .apply method that requires an object of type <[_1, _3]. This implies
  * 'inductive' has produced <[_1,_3] object and implicitly it requires an object of type <[_0,_2]. And <[_0,_2] is
  * produced by 'lessThanBasic' which requires _1 of type Nat.
  * */

  trait <=[A <: Nat, B <: Nat]
  object <= {
    implicit def ltEquals[B <: Nat]: <=[_0, B] = new <=[_0, B] {}
    implicit def inductive[A <: Nat, B <: Nat](implicit lte: <=[A,B]): <=[Succ[A], Succ[B]] = new <=[Succ[A], Succ[B]] {}
    def apply[A <: Nat, B <: Nat](implicit lte: <=[A,B]): <=[A,B] = lte
  }

  val compEquals: <=[_1,_1] = <=[_1,_1]
  // val invalidComp: _3 <= _1 = <=[_3,_1] This won't compile

  // Add numbers as type
  trait +[A <: Nat, B <: Nat, S <: Nat]
  object + {

    // As a starting point, defining 0 + 0 = 0
    implicit val zero: +[_0, _0, _0] = new +[_0, _0, _0] {}

    /*
    * For any number A <: Nat such that A > 0, A + _0 = A. An instance of type <[_0, A] to show A > 0 is required since
    * an object of type +[A, B, C] can be made by compiler in different ways, but we're imposing a condition by which the
    * compiler should make the object implicitly available
    * */
    implicit def basicRight[A <: Nat](implicit lt : _0 < A) : +[_0, A, A] = new [_0, A, A] {}
    implicit def basicLeft[A <: Nat](implicit lt : _0 < A) : +[A, _0, A] = new [A, _0, A] {}// Cumulative property: A + 0 = 0 + A
    def apply[A <: Nat, B <: Nat, S <: Nat](implicit plus : +[A, B, S]) : +[A, B, S] = plus
  }

  val zeroSum : +[_0, _0, _0] = +.apply
  val numSum : +[_3,_0,_3] = +.apply
}
