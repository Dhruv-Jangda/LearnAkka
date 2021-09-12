package com.learnakka

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.TestProbe
import scala.concurrent.duration._
import akka.testkit.TestKit
import akka.testkit.ImplicitSender

object SimpleActorTest extends App {

  /**
   * Akka Testkit - Central object is TestProbe, that acts as a remote controlled actor. It buffers the incoming
   * messages so that they can be tested
   */
  class Toggle extends Actor {
    def happy: Receive = {
      case "How are you?" ⇒
        sender() ! "happy"
        context become sad
    }
    def sad: Receive = {
      case "How are you?" ⇒
        sender() ! "sad"
        context become happy
    }
    def receive = happy
  }

  // Running a TestProbe from the outside
  // ------------------------------------

  // Step 1 - Create an ActorSystem and make it implicitly available. An ActorSystem comes with a Guardian Actor
  implicit val system = ActorSystem("TestSys")

  /**
   * Step 2 - Create an Actor under the ActorSystem. Behind the scenes, system.actorOf is same as context.actorOf
   * where context is of the Guardian Actor
   */
  val toggle = system.actorOf(Props[Toggle])

  // Step 3 - Create a TestProbe. This creates the actor in step 2 for which an ActorSystem has to be implicitly available
  val p = TestProbe()

  // Step 4 - Perform message checking using TestProbe created above
  p.send(toggle, "How are you?")
  p.expectMsg("happy")
  p.send(toggle, "How are you?")
  p.expectMsg("sad")
  p.send(toggle, "unknown")
  p.expectNoMessage(1.second)

  // Step 5 - After message testing, shut down the Actor System
  system.terminate()

  // Running TestProbe from inside a TestKit
  // ---------------------------------------
  new TestKit(ActorSystem("TestSys")) with ImplicitSender {

    // Inside a TestKit, an Actor System is available as "system". With ImplicitSender a default Actor is available
    // implicitly and is picked up when we send messages. The sender is available as "testActor"
    val toggle = system.actorOf(Props[Toggle])
    toggle ! "How are you?"

    // expectMsg() is directly available in TestKit i.e. it is not necessary to provide probe.expectMsg()
    expectMsg("happy")
    toggle ! "How are you?"
    expectMsg("sad")
    toggle ! "unknown"
    expectNoMessage(1.second)
    system.terminate()
  }

  println("done")

  /**
   * If an actor is accessing a DB or a production service and we don't want to use the real DB or service for testing.
   * One simple solution is to add override factory methods or use dependency injection(i.e. using different db handles
   * during testing, staging and production). This can be done with Akka as well, eg: Akka with Spring or -
   *
   * Eg: Controller Actor has access to a DB or a production service, but we want to test it using Receptionist Actor.
   * So rather than hard wiring Props[Controller] in context.actorOf(), we can create a method in the Receptionist that
   * returns Props[Controller] and can override the same while testing so that we don't actually use the production
   * services provided directly by the Controller. We create a fake data to mimic the production service functionality
   * and use it while testing utilizing the methods of Controller.
   *
   * class Receptionist extends Actor {
   *  def controllerProps : Props = Props[Controller] <- Like this and use this method in context.actorOf() as well.
   *
   *  def receive = {
   *    ...
   *    val controller = context.actorOf(Props[Controller], "controller")
   *    ...
   *  }
   * }
   *
   * A Guardian Actor is already available when we create an ActorSystem but it is not possible to check the messages
   * send back to the Guardian. Hence, sometimes we need to create an Actor(a StepParent) that works in place of Guardian
   * Actor but is capable to access the send messages.
   *
   * class StepParent(child: Props, probe: ActorRef) extends Actor {
   *  context.actorOf(chile, "child")
   *  def receive = {
   *    case msg => probe.tell(msg, sender) // or probe.forward(msg)
   *  }
   * }
   *
   * .tell() sends the message to child keeping the original sender as is i.e. when msg is received at the child,
   * it appears to come from the original sender than the StepParent
    */

  /**
   * To test an Actor hierarchy, each actor has to be tested in isolation, creating Fake Actors to which it communicates
   * using bottom-up approach. From leaves then spawning supervisor actors and testing them all together => Integration
   * testing of the actors. <- Reverse Onion Testing
    */

}