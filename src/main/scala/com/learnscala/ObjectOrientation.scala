package com.learnscala

object ObjectOrientation extends App {
  // Class name is itself a constructor. Class parameters vs fields: val/var declared before a parameter makes it a class field
  class Car(var name: String) {

    // A function can be left just undefined i.e. without any body, in an abstract class and not in a normal class.
    def drive() = print("Hi, I'm a normal class!")
  }

  // Class parameters can't be accessed by an object of the class, but fields can
  val car: Car = new Car("Audi")

  /*
    Abstract class vs Traits: Abstract class and traits can't be instantiated. A class can inherit from a single abstract class but from
    multiple traits(called "mixing"). Abstract classes are faster in execution than traits, since a class gets compiled as a copy to a byte
    code and used directly during execution. Abstract classes defined in Scala can be used directly in Java but a trait shouldn't have a body
    to be used directly in Java. Parameters can be passed to abstract class but not to trait/object.A trait can inherit another trait or abstract
    class and vice-versa. During inheritance, functions declared in abstract class/trait have to be defined in child class.
    Multiple inheritance syntax - class A extends X with Y...
   */
  abstract class Vehicle {
    var numWheels: Int = 4
    def gearRatios(ratio: String): Unit
  }

  /*
    When defining the function of abstract class in the child, "override" keyword is used. Function can be defined with any name. Eg: "?!"
    is a valid method name => Using infix notation 4 + 5 equivalent to 4.+(5), "+" is actually a method. Hence operators in scala are methods only.
   */
  class Truck extends Vehicle {
    this.numWheels = 8
    override def gearRatios(ratio: String): Unit = print(
      s"Gear ratios of truck - $ratio"
    )
  }

  // Infix notation : <object> <function> <argument> is same as <object>.<function>.<arguments> - Valid for methods with ONE argument
  val aTruck: Truck = new Truck()
  aTruck.gearRatios("5:4")
  aTruck gearRatios "5:4"

  /*
    Anonymous classes: Defined by just using a variable. Below code complied as:
    class Truck_Anonymous_78954 extends Truck {
        override def gearRatios(ratio: String): Unit = print(s"Gear ratios of another truck - $ratio")
    }
    val anotherTruck = new Truck_Anonymous_78954()
   */
  val anotherTruck = new Truck {
    override def gearRatios(ratio: String): Unit = print(
      s"Gear ratios of another truck - $ratio"
    )
  }

  /*
    Singleton object - "Type" and "Object" directly defined and the <object> is of <type> defined. Eg: MySingleton is the only instance of
    MySingleton type. Scala provides a special method "apply" that can be defined in any class/object, can take any parameters and can be
    used directly as <object>(<parameter(s)>). This makes it very useful in functional programming aspect of Scala.
   */
  object MySingleton {
    val singletonValue: Int = 456
    def singletonMethod(num: Int): Int = 2 ^ num
    def apply(num: Int): Int = num ^ 2 // Special method
  }

  MySingleton.singletonMethod(10)
  // Both below are equivalent
  MySingleton.apply(5)
  MySingleton(5)

  /*
    Companion Object - Object defined with same name as of a class/trait defined in the same scope. Eg: Here class Car is defined, so
    object Car can also be defined. Property - Companions can access each other's private fields/methods. But Singleton Car and object
    of class Car are different. In practice, when companion objects are present in the scope, we don't instantiate the class, rather
    we use companions to class's methods/variables and not object's
   */
  object Car {}

  /*
    Case classes: lightweight data structures with some boilerplate. When compiled, following gets generated for a case class:
    - Sensible equals and hash code.
    - Serialization
    - Has Companion object with apply => can be instantiated without keyword "new"
    - Can be used in Pattern Matching
   */
  case class Person(name: String, age: Int)
  val samarth: Person =
    Person("Samarth", 21) // Equivalent to Person.apply("Samarth", 21)

  // Exception Handling
  try {
    val aNullString: String = null
    aNullString.length()
  } catch {
    case e: Exception => "Some error message!"
  } finally {
    // Exception thrown or not, the block will execute!
    println(
      "So useful to shed all dependet resources that may be left open. Eg: closing files, connections, etc."
    )
  }

  /*
    Generics - Data Structures that ensures type safety. The below class can be used for a <type> of variable => saves re-doing all the
    work for a new <type> i.e. no need to define all methods/variable for type String again, when same can be used for type Int -> REUSABILITY
   */
  class MyList[T] {}
  val aListOfInt: MyList[Int] = new MyList[Int]
  val aListOfStr: MyList[String] = new MyList[String]

  /*
    Important Points
    1. In Scala we usually operate with IMMUTABLE values/objects i.e. any modification to an object of a class MUST return another object. Benefits:
      a. Speeds up development tremendously in a multi-threaded/distributed environment.
      b. Helps making sense of the code.("reasoning about")
   */
  val aDummyList: List[Int] = List(1, 2, 3)
  val reverseList: List[Int] =
    aDummyList.reverse // Modification to object aDummyList, returns another object

  /*
    2. Scala is closest to OO ideal. Everything is declared inside an object or a class => everything even method is an object in Scala
   */
}
