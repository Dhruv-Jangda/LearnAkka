package com.learnscala

object PatternMatching extends App {
  // Pattern Matching(PM) is actually EXPRESSION => can be reduced to a VALUE. Matches values IN SEQUENCE
  val anInteger: Int = 50
  val order: String = anInteger match {
    case 1 => "first"
    case 2 => "second"
    case 3 => "third"
    case _ => s"$anInteger th"
  }

  // PM can be used to match a whole data structure. Here: matching value to type Person
  case class Person(name: String, age: Int)
  val bob: Person = Person("Bob", 34)
  // Once match happens, the data of the data structure can be accessed <- Deconstruction of data structure
  val matchedPerson: String = bob match {
    case Person(n, a) => s"Hi! My name is $n and I am $a years old."
    case _ => "Not a person!"
  }

  // Deconstructing Tuples
  val aTuple : (String, String) = ("Linkin Park", "Papercut")
  val matchedTuple : String= aTuple match {
    case (band, song) => s"$song is composed by $band"
    case _ =>
      "Can't say!" // The default case, best practices since a MatchException is thrown when nothing matches
  }
}
