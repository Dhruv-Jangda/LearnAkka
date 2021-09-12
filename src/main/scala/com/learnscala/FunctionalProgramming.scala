package com.learnscala

object FunctionalProgramming extends App {
  class Person(val name: String) {
    def apply(age: Int): Unit = println(s"${this.name} has aged $age years")
  }

  val samarth: Person = new Person("samarth")
  samarth.apply(21)
  samarth(21) // Invoking instance of a class like a function

  /*
    Scala runs on JVM, fundamentally made for Java or an OO language. So JVM knows what class is but not functions as first class citizen.
    In Functional Programming, we treat functions as first class citizen/elements of a class
   * Compose functions i.e. working with functions just like any other variable
   * Pass functions as arguments
   * Return functions as results

    Conclusion: Amazing! Making functional programming explicitly as as OO programming by "FunctionX"
   */

  // Function1 is a simple trait that takes "Int" and returns "Int"
  val simpleIncrementor = new Function1[Int, Int] {
    override def apply(arg: Int): Int = arg + 1
  }
  /*
    Observe that simpleIncrementor is actually an object but is used here as function - Basically acting like a Function
    Conclusion - All SCALA FUNCTIONS are INSTANCES of FUNCTION_X type
    FunctionX - X imply #arguments the function takes in. X: [0,22]
   */
  simpleIncrementor(20) // simpleIncrementor.apply(20) - returns 21

  // Syntax Sugars i.e. alternative codes that reduces such boiler plates as in definition of simpleIncrementor
  val doubler: Function1[Int, Int] = (arg: Int) => 2 * arg
  doubler(3) // result - 6

  /*
    Above code is actually equivalent to longer:
    new Function1[Int, Int] {
        override def apply(arg: Int): Int = 2 * arg
    }
    Another syntax sugar for above code, line #34 i.e.
    val square = new Function1[Int, Int] {
       override def apply(arg: Int): Int = arg ^ 2
    }
   */
  val square: Int => Int = (arg: Int) => arg ^ 2
  square(23)
  // Another syntactic sugar - val square = (arg: Int) => arg ^ 2

  /*
    HOF - Higher Order Functions that (takes in function) OR (returns functions) OR (both)
    Eg: Below map and flatMap function expects a function as input, hence a HOF.
   * Observe format "x => func(x)" is an anonymous function
   * flatMap - Actually produces [[1,2], [2,4], [4,16]] flattened to [1, 2, 2, 4, 4, 16]
   * .map( <func> ) equivalent to .map{ <func> } i.e () used interchangeably with {}
   * .foreach( <func> ) is NOT a loop but HOF
   */
  val aMappedList: List[Int] = List(1, 2, 4).map(x => x * 2)
  val aFlatMappedList: List[Int] = List(1, 2, 4).flatMap(x => List(x, x * x))
  val aFilteredList: List[Int] = List(1, 2, 3, 4, 5).filter(x =>
    x <= 3
  ) // Syntactic Sugar for (x => x <= 3) is (_ <= 3)

  /*
  We use immutable variables as a good practice in Scala i.e. every above functions .map, .flatMap and .filter returns another instance of List[Int]
  which is different from what the function was applied on to. This lets us CHAIN functions. Eg: We need to have all pairs between (1,2,3) and (a,b,c)
   */
  val aChainedList = List(1, 2, 3).flatMap(num =>
    List("a", "b", "c").map(char => s"$num - $char")
  )

  // For large chained application of functions - "for comprehensions". Eg: for comprehension of above example
  val alternativeList = for {
    num <- List(1, 2, 3)
    char <- List("a", "b", "c")
  } yield s"$num - $char"

  // LISTS
  val aList: List[Int] = List(1, 2, 3, 4, 5)
  val firstElement: Int = aList.head
  val remainingList: List[Int] = aList.tail
  val aPrependedList: List[Int] = 0 :: aList // List(0,1,2,3,4,5)
  val anExtendedList: List[Int] = 0 +: aList :+ 6 // List(0,1,2,3,4,5,6)

  // SEQUENCES - elements can be accessed at any index
  val aSequence: Seq[Int] = Seq(1, 2, 3)
  val accessedElement: Int = aSequence(2) // Returns an element at index 2 i.e 3

  // VECTORS - Sequences only, but faster in implementation
  val aVector: Vector[Int] = Vector(1, 2, 3, 4, 5)

  // SETS - Collection with no duplicates. Fundamentally used to check if an element is present in the set.
  val aSet: Set[Int] =
    Set(1, 2, 3, 4, 1, 2, 3) // This actually gives aSet = Set(1,2,3,4)
  val aSetHas5: Boolean = aSet.contains(5) // returns false
  val anAddedSet: Set[Int] =
    aSet + 5 // returns Set(1,2,3,4,5) - Order is not important in a set
  val aRemovedSet: Set[Int] = aSet - 3 // returns Set(1,2,4,5)

  // RANGES - Specifically used for iteration. All COLLECTION objects are INTERCONVERTIBLE by calling .toList, .toSet, etc
  val aRange = 1 to 1000
  val twoByTwo =
    aRange
      .map(_ * 2)
      .toList // returns List(2,4,6,8,...,2000). "_" is syntactic sugar for "x => x"

  // TUPLES - Group of any type of values
  val aTuple = ("Hi", "Me", 2)

  // MAPS - Data Structure of (key, value)
  val aMap: Map[String, Int] = Map(
    "samarth" -> 21,
    ("dhruv", 28) // Equivalent to "dhruv" -> 28
  )
}
