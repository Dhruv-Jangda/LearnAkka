package com.learnscala

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.Success

object Monads extends App{

  /*
    Monads: Construction that performs successive calculations. It is an object which wraps the other object. Two fundamentals
    * Ability to wraps a value to other value or itself. Eg: "internalVal" is being wrapped to "SafeValue". In OO "SafeValue"
    is called "constructor" while in FP(functional programming) it is "pure" or "unit"
    * Ability to transform a type of value to another type of value by based on a transformation function. Eg type
    of "Scala does wonders!" i.e. String being transformed to SafeValue[<type>] using "transformer" as input function.
  */

  // Class to prevent multi-threaded access to a value. Act as a wrapper to return some value
  case class SafeValue[+T](private val internalVal: T) {
    // below method act as a place holder of a value
    def get: T = synchronized(
      internalVal
    )

    /*
     single ETW function to mimic the operations. In Scala, below function is called "bind" or "flatMap".
     So better to rename to flatMap
     */
    def transform[S](transformer: T => SafeValue[S]): SafeValue[S] = synchronized(
      transformer(internalVal)
    )
  }

  // Assume the function being available as an external API & its implementation is not known
  def giveMeSafeValue[T](value: T): SafeValue[T] = SafeValue(value)

  // To access the internal value "Scala does wonders!" and turn to uppercase. ETW Pattern
  val safeString: SafeValue[String] = giveMeSafeValue("Scala does wonders!")
  val newString = safeString.get // Extract(E)
  val transformString = newString.toUpperCase() // Transform(T)
  val upperSafeString = SafeValue(transformString) // Wrap(W)

  // ETW operation with one call <- Feasible "Monad"
  val etwOp = safeString.transform(s => SafeValue(s.toUpperCase()))

  /*
    Example 1 - Real life Monad usage via Census API
    Let's say we have a modal class to mimic database table Person with first name and last name as its fields. Now we
    have an API to fill it up with the entries from the external user
    */

  /*
    Model class Person. Before entering to DB, performing a check if firstName & lastName are not null. Another problem - This
    class will be provided with the parameters from another API and that API will be from external user => the parameters
    firstName & lastName MAY be nulls. Hence null handling has to take place at the API
    */
  case class Person(firstName: String, lastName:String) {
    assert(firstName!=null && lastName!=null)
  }

  // Generally this is how handling will take place
  def getPerson(firstName: String, lastName: String): Person = {
    if (firstName != null) {
      if (lastName != null)
        Person(firstName, lastName)
      else
        null
    }
    else
      null
  }

  /*
    The above method can be handled in a better fashion by using "Option". Also observe that the flatMap below makes
    sure the final return to be of type Option in a similar fashion as transform(also called "flatMap") method above.
    */
  def getPersonBetter(firstName: String, lastName: String): Option[Person] = {
    Option(firstName).flatMap(
      fName => Option(lastName).flatMap(
        lName => Option(Person(fName, lName))
      )
    )
  }

  // Observe how Option takes care of null safety similarly like thread safety in class SafeValue
  def getPersonForComprehension(firstName: String, lastName: String): Option[Person] = for {
    fName <- Option(firstName)
    lName <- Option(lastName)
  } yield Person(fName, lName)

  // Example 2 - Asynchronous fetches via online store App. Tip: Never define price in type "Double"
  case class User(id: String)
  case class Product(sku: String, price: Int)

  def getUser(url: String) : Future[User] = Future(
    User("daniel")
  )

  def getLastProduct(userId: String) : Future[Product] = Future(
    Product("abc-123", 99)
  )

  val danielUrl = "my.store.com/users/daniel"

  /*
    Now the task is to send a mail as a notification for last purchase to a user. So how to get the last product
    ordered by a user given his/her "id". Also knowing the functions work in asynchronous manner, they have to be
    called synchronously i.e. one after other since "getLastProduct" takes in "id" which is fed to class "User".
    This way we're doing ETW repetitively <- Bad practice as hard to read, doesn't cover all the cases(we do need
    to account for failures as well).
    */
  getUser(danielUrl).onComplete {
    case Success(User(id)) =>
      getLastProduct(id).onComplete{
        case Success(Product(sku, price)) => price * 10
      }
  }

  // All this can be covered with .flatMap over Future type
  val vatInclPrice : Future[Int] = getUser(danielUrl).flatMap(
    user => getLastProduct(user.id).map(
      product => product.price * 10
    )
  )

  // Using For comprehensions
  val vatInclPriceFor : Future[Int] = for {
    user <- getUser(danielUrl)
    product <- getLastProduct(user.id)
  } yield product.price * 10

  /*
  Hence all ETW patterns can be resolved to FLATMAP and MAP structures.
  Monads exhibit certain properties.
  */

  // Property 1: Left Identity - Monad(x).flatMap(<func>) equals <func>(x)
  def twoConsecutive(x: Int) = List(x, x+1)
  twoConsecutive(4) // List(4,5)
  List(4).flatMap(twoConsecutive) // List(4,5)

  // Property 2: Right Identity - Monad(v).flatMap(x => Monad(x)) equals Monad(v). This is USELESS
  List(1,2,3).flatMap(x => List(x)) // List(1,2,3)

  // Property 3: Associativity i.e. ETW-ETW-ETW-...
  val numbers : List[Int] = List(1,2,3)
  def incrementer(x: Int) = List(x, x+1)
  def doubler(x: Int) = List(x, x*2)
  numbers.flatMap(incrementer).flatMap(doubler)
  /*
  equals numbers.flatMap(x => incrementer(x).flatMap(doubler)), but here the function defined in flatMap is composite
  working => List(1,2,3) -> incrementer: List(1,2,2,3,3,4) -> doubler -> List(1,2,2,4,2,4,3,6,3,6,4,8)
  List(
    incrementer(1).flatMap(doubler) => 1,2,2,4
    incrementer(2).flatMap(doubler) => 2,4,3,6
    incrementer(3).flatMap(doubler) => 3,6,4,8
  )
  Monad(v).flatMap(f).flatMap(g) - f applied to every element in v, combined, then g applied to every element of the
  combined. Equals Monad(v).flatMap(x => f(x).flatMap(g)) - f.flatMap(g) applied to every element in v then combined
  In other words, operations to monads are applied sequentially.
  */

  // Futures are inherently non-deterministic. Given service calls its API asynchronously
  object MyService {
    def producePreciousValue(urArg: Int) : String = "The meaning of life is " + (urArg/42)

    /*
    The method will run "function" asynchronously on the passed "actualArg". And assuming we can't change its
    definition. So the problem is "function" runs on another thread and we can't get the return values evaluated on
    the thread. This implies that the API is fixed in production but not the point("when") of "function" call. Hence,
    it'll be wrong to implement giveMePreciousValue = Future{ MyService.producePreciousValue(arg) }, since we don't know
    when will the call to producePreciousValue() will be executed
    */
    def submitTask[A](actualArg: A)(function: A => Unit) : Boolean = { true }
  }

  /*
  Hence fiveMePreciousValue can be implemented using "Promise" on the MyService's APIs. "Promise" helps control
  "Future" and is a wrapper around it.
  */

  // Step 1: Create a Promise
  val myPromise = Promise[String]

  // Step 2: Extract its Future
  val myFuture = myPromise.future

  // Step 3: Consume the Future
  val furtherProcessing = myFuture.map(_.toUpperCase())

  // Step 4: Pass the Promise to the producer of the value
  def asyncCall(promise: Promise[String]): Unit = {
    // When this promise is completed, the future it contains is also completed with the same value
    promise.success("Your value here, your majesty!")
  }

  // Step 5: Call the producer
  asyncCall(myPromise)

  // Task is to implement below method given "MyService" using the above steps
  def giveMePreciousValue(arg: Int) : Future[String] = {
    // Step 1: Create a Promise
    val thePromise = Promise[String]()

    // Step 5: Call the producer, so that Future within "thePromise" is updated accordingly
    MyService.submitTask(arg) { x : Int =>

      // Step 4: Pass the Promise to the producer of the value
      val preciousVal = MyService.producePreciousValue(x)
      thePromise.success(preciousVal)
    }

    // Step 2: Extract the future
    thePromise.future
  }

  // Step 3: Consume the Future - As soon as the Promise above will complete with a success, the future here will be automatically consumed
}
