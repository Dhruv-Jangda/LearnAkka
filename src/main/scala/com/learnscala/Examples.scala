package com.learnscala

import scala.annotation.tailrec
import scala.concurrent.{Future, Promise}
import scala.util.Try

object Examples extends App {

  // 1. Common tail recursive(i.e. recursive call is the last thing done by a function) functions used in collections
  def sum(ls: List[Int]): Int = ls match {
    case x :: xs => x + sum(xs) // Here x has been used with the call, so not tail recursive
    case Nil => 0
  }

  @tailrec
  def max[Int](ls: List[Int], maxNum: Int): Int = ls match {
    case x :: xs => max(xs, if (x >= maxNum) x else maxNum)
    case Nil => maxNum
  }

  /*
   * class List[+T] {
   *  def map[U](f: T => U): List[U] = this match {
   *    case x :: xs => f(x) :+ xs.map(f)
   *    case Nil => Nil
   *  }
   *
   *  def flatMap[U](f: T => List[U]): List[U] = this match {
   *    case x :: xs => f(x) ++ xs.flatMap(f)
   *    case Nil => Nil
   *  }
   *
   *  def filter[U](f: T => Boolean): List[U] = this match {
   *    case x :: xs => if (f(x)) x :+ xs.filter(f) else xs.filter(f)
   *    case Nil => Nil
   *  }
   * }
   */

  /* 2. Representing JSON
   * {
   *  "firstName" : "John",
   *  "lastName" : "Smith",
   *  "address" : {
   *    "streetAddress" : "21 2nd Street",
   *    "state" : "NY",
   *    "postalCode" : 10021
   *  },
   *  "phoneNumbers" : [
   *    {"type" : "home", "number" : "212 555-1234"},
   *    {"type" : "fax", "number" : "646 555-4687"}
   *  ]
   * }
   */
  abstract class JSON
  case class JSeq (elems: List[JSON]) extends JSON
  case class JObj (bindings: Map[String, JSON]) extends JSON
  case class JNum (num: Double) extends JSON
  case class JStr (str: String) extends JSON
  case class JBool (bool: Boolean) extends JSON
  case object JNull extends JSON

  // Creating the JSON
  val data : JSON = JObj(Map(
    "firstName" -> JStr("John"),
    "lastname" -> JStr("Smith"),
    "address" -> JObj(Map(
      "streetAddress" -> JStr("21 2ns Street"),
      "state" -> JStr("NY"),
      "postalCode" -> JNum(10021)
    )),
    "phoneNumbers" -> JSeq(List(
      JObj(Map(
        "type" -> JStr("home"),
        "number" -> JStr("212 555-1234")
      )),
      JObj(Map(
        "type" -> JStr("fax"),
        "number" -> JStr("646 555-4687")
      ))
    ))
  ))

  // Parsing the JSON data
  def showJSON(json: JSON): String = json match {
    case JSeq(elems) => "[" + elems.map(showJSON).mkString(",") + "]" // alternate syntax: (elems map showJSON) mkString ","
    case JObj(bindings) => "{" + bindings.map(binding => binding._1 + ":" + showJSON(binding._2)).mkString(",") + "}"
    case JStr(str) => str
    case JNum(num) => num.toString
    case JBool(bool) => bool.toString
    case JNull => null.toString
  }


  // 3. Asynchronous functions with dependency - Providing call back to function as

  trait A
  trait B

  def program(a: A): B // Synchronous definition
  def program(a: A, func: B => Unit): Unit // Asynchronous definition

  /*
   * To handle exceptions & failures better the function "func" should be -> func : Try[B] => Unit. Then we can handle
   * Success(<value>) & Failure(<value>) differently. That way the "program" would work according to "func" i.e. working
   * independently(asynchronously)
   */
  def program(a: A, func: Try[B] => Unit): Unit

  /*
   * If we introduce Future to synchronous definition, it gets evaluated in another thread & so can be used in
   * asynchronous fashion
   */
  def program(a: A): Future[B]

  /*
   * Even Future propagates the exceptions caused in any thread further. Hence "recover"(synchronous) and
   * "recoverWith"(asynchronous) in Future are helpful to handle exceptions.
   */
  def recover[B >: A](pf: PartialFunction[Throwable, B]): Future[B]
  def recoverWith[B >: A](pf: PartialFunction[Throwable, Future[B]]): Future[B]

  /*
   * grindBeans().recoverWith {
   *  case BeansBucketEmpty => refillBeans().flatMap(_ => grindBeans())
   * }.flatMap(coffeePowder => brew(coffeePowder))
   *
   * But here if second call to grindBeans() fail, we're not recovering from it.
   *
   * "Promise" is a writable, single-assignment container, which completes a "Future" while "Future" is defined as a
   * type of read-only placeholder object created for a result which doesn't yet exist. Promise can complete a future by
   * either Success or Failure
   */
  trait Coffee
  def makeCoffee(
                  coffeeDone: Coffee => Unit,
                  onFailure: Exception => Unit
                ): Unit

  def makeCoffee2(): Future[Coffee] = {
    val p = Promise[Coffee]()
    makeCoffee(
      coffee => p.trySuccess(coffee),
      reason => p.tryFailure(reason)
    )
    p.future
  }
}
