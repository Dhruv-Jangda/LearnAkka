package com.learnakka

import akka.actor.SupervisorStrategy.{Escalate, Restart, Stop}
import akka.actor.{Actor, ActorKilledException, ActorRef, OneForOneStrategy, Props, Stash}
import akka.event.Logging.LogEvent

import scala.concurrent.duration.DurationInt

/*
* An actor can be imagined as person and not as an object of some kind, that has:
* 1. Identity - Reference address default provided by "ActorRef"
* 2. Behaviour
* 3. Only interacts using asynchronous message passing. Eg: if ActorA sends message to ActorB, then after sending it will
* continue its work regardless of ActorB replying back to ActorA.
* */

object LearnAkka extends App {

  /*
  * Internally Actor trait is defined as:
  *
  * "Receive" describes partial function as a response to a message send by sender but never returns(output type "Unit"),
  * as the sender is long gone(asynchronous message passing)
  * type Receive = PartialFunction[Any, Unit]
  *
  * trait Actor {
  *   implicit val self: ActorRef // Any actor will have its identity/address implicitly available
  *   implicit val context: ActorContext
  *   def receive: Receive
  *   def sender: ActorRef
  *   ...
  * }
  *
  * abstract class ActorRef {
  *   // Current actor's ActorRef is passed implicitly in variable sender, which is available to receiving Actor implicitly.
  *      This is what sender - line#30 depicts
  *   def !(msg: Any)(implicit sender: ActorRef = Actor.noSender): Unit // Implemented as "tell" in Java
  * }
  *
  * trait ActorContext {
  *   // Due to "become" type "Receive" -> "Unit", it can be used in receive function of Actor. The default mode of operation
  *   // is to replace the current behaviour by the top most one passed.
  *   def become(behaviour: Receive, discardOld: Boolean = true): Unit
  *   def unbecome(): Unit
  *   def actorOf(p: Props, name: String): ActorRef // Is used to create another actor. Hence can from a hierarchy
  *   def stop(a: ActorRef): Unit // To stop the created actor and is often applied to self => Actor decides if it wants to terminate
  *   def children: Iterable[ActorRef]
  *   def child(name: String): Option[ActorRef]
  * }
  */

  trait Counter extends Actor {
    var count = 0

    def receive: PartialFunction[Any, Unit] = {
      case "incr" => count += 1
      /*
      * Issues with this Actor:
      * 1. Just sends message but never gets a response.
      * 2. Stateless actor due to no Reference or Identity provided
      * 3. One never gets to know the value of count
      * To counter these:
      * case ("get", customer: ActorRef) => customer.!(count) // or customer ! count
      * But since, the sender's address gets implicitly copied in "sender", the "sender" can be used directly
      * */
      case "get" => sender ! count
    }
  }

  trait CounterBetter extends Actor {
    def counter(n: Int): Receive = {
      case "incr" => context.become(counter(n + 1))
      case "get" => sender ! n
    }

    def receive: Receive = counter(0)
  }

  /*
  * Advantages:
  * 1. There is only one place where state is changed - .become(<state>). Very clear and explicit.
  * 2. The state is scoped to current behaviour i.e. there is no variable that can be left in an unknown state
  * The "counter" method of "CounterBetter" is kind of tail recursive, asynchronously.
  *
  * Actor's state can be modified only by sending messages and they run concurrently, independently => (Encapsulated &
  * completely isolated from each other). Actor's internally are evaluated as single thread. This way their current state
  * and behaviour remains synchronised.
  */

  class ActorExample extends Actor {
    val counter = context.actorOf(Props[CounterBetter], "counter")

    // By default the call goes to "receive" method
    counter ! "incr"
    counter ! "incr"
    counter ! "incr"
    counter ! "get"

    def receive: PartialFunction[Any, Unit] = {
      // Accessing current state "count" of the Actor "counter"
      case count: Int =>
        print(count)
        context.stop(counter)
    }
  }

  /*
   * Whenever an exception arises in an Actor System, it is delegated and send to its Supervisor, but then how the actor's
   * state will be handled ?
   *
   * Actually in an asynchronous system, the issue in failure handling is where the communicated failures should go. Eg:
   * In a synchronous application the exception occurred by a method is returned back to the caller, but in an Actor
   * System, an actor sends the message and moves on. So where will the exception if occurred will be communicated ?
   * Just like other messages are communicated, the exception can be sent as a message to a known address.
   *
   * An Actor model is anthropomorphic in nature:
   * i. An Actor system works like a team.
   * ii. Any failure within the system is communicated to its Team Leader i.e the parent that create other actors.
   * iii. Then the failure is needed will be communicated back to the leader's parent. This way every failure is
   * eventually handled
   * i.e The system is just like a hierarchical system.
   *
   * Resilience - Recovering from exceptions back to the normal form. To achieve this:
   * i. Containment i.e. a failure is isolated and doesn't spread to other components. Taken care by an Actor
   * automatically due to being fully encapsulated.
   * ii. The failure has to be delegated to another Actor, actually to a Supervisor(or Parent).
   *
   * Hence, the creation and supervision hierarchy are same in Akka - termed as "Mandatory Parental Supervision".
   */

  class DBActor extends Actor {
    import Manager._
    def receive: Receive = {
      case DBException => sender ! DBException
    }
  }

  class ImportantServiceActor extends Actor {
    import Manager._
    def receive: Receive = {
      case ServiceDownException => sender ! ServiceDownException
    }
  }

  object Manager {
    case object DBException
    case object ServiceDownException
  }

  class Manager extends Actor {
    import Manager._
    var restarts = Map.empty[ActorRef, Integer].withDefaultValue(0)

    /*
     * In Akka default decision is to restart all child actors when they fail. Here overriding the default.
     * OneForOneStrategy and VarianceStrategy are used to handle decision. Both takes decider with is match casing code
     * block for different decisions. "Restart", "Stop" and "Escalate" are the inbuilt strategies in Actor model that
     * restarts, stops the child Actor and escalates(or sends) the exception to its Supervisor respectively.
     *
     * This supervisorStrategy has full access to the Actors state. Also no normal message is processed when a exception
     * based message is getting processed <- (Important)
     */
    override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy() {
      // case _ : DBException => Restart <- Old Strategy, modifying after knowing (Important) above
      case _ : DBException =>
        restarts(sender) match {
          case tooMany : Integer if tooMany > 10 => restarts -= sender;Stop
          case n : Integer => restarts.updated(sender, n+1);Restart
        }
      case _ : ActorKilledException => Stop // "ActorKilledException" is thrown by "actor ! Kill"
      case _ : ServiceDownException => Escalate
    }

    /*
     * One point to note - superVisorStrategy can also be defined in def, but that will initiate the strategy everytime
     * on each failure which is not efficient.
     */

    context.actorOf(Props[DBActor], "db")
    context.actorOf(Props[ImportantServiceActor], "service")
  }

  /*
   * OneForOneStrategy() - decision is applied to each actor in isolation separately.
   * AllForOneStrategy() - decision is applied to all children of the supervisor, not only the failing one i.e.
   * supervising a group of actors that live/die together
   * Both strategies can be defined with a finite number of restarts in a finite window
   * OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) -> "Restart" turns to "Stop" if either of the
   * limit(time window or number of retries) crosses.
   *
   * If an Actor is restarted, what happens to its ActorRef ? As this may trouble other actors communicating with the
   * actor restarted. Actually the previous actor object is deleted on restart but not its ActorRef.
   *
   * Actor Lifecycle: Refer Lecture 3.1, timestamp -> 15:17
   *
   * Note:
   * 1. No messages will be processed between the actor failure and till its restart(assuming the supervisor sends
   * message to restart) and the processing will start from the subsequent message post the one causing failure.
   * 2. At critical points such as "Start", "Restart", "Stop", some actor hooks corresponding to each such point will be
   * called. Eg: preStart() is called after Actor is instantiated but before processing any message, preRestart(),
   * postRestart(), postStop(), etc.
   *
   * trait Actor {
   *    def preStart(): Unit = {} // called before processing any new message, though by the time the Actor is ready to
   *                                 process the messages they will get buffered
   *    def preRestart(reason: Throwable, message: Option[Any]): Unit = {
   *      context.children foreach (context.stop(_))
   *      postStop()
   *    }
   *    def postRestart(reason: Throwable): Unit = {
   *      preStart()
   *    }
   *    def postStop(): Unit = {}
   *    ...
   * }
   *
   * 3. An actor has only one Start and Stop event. Rest events "Restart", etc can happen any number of times.
   * 4. There could be situations where we need to override these methods to modify the default behaviour. Eg: An actor
   * handling database connection should close the same when postStop() is called. Eg: An actor handling an external
   * object might need to register itself during the preStart() and unregister during postStop(), though should not
   * stop all its children as the actor is handling external object which is independent of its child actors. In such a
   * case preRestart() and postRestart() should do nothing.
   */

  /*
   * Actor Lifecycle monitoring - An actor can't really tell the difference between the other actor being terminated or
   * not replying. To handle this, "DeathWatch" a feature in Actor exists to monitor the lifecycle of an Actor. Any actor
   * can register its interest in monitoring the lifecycle of an actor for which it has the ActorRef.
   *
   * The DeathWatch API has methods - watch(<ActorRef>):ActorRef and unwatch(<ActorRef>):ActorRef. Akka provides a
   * special message "Terminated" which is private to Akka itself that prohibits the code to use companion object's
   * apply method. "Terminated" takes argument ActorRef of the actor terminated and two flags -
   * 1. First - whether the existence of the actor could be confirmed by the system. So flag is sent "true" to the watcher
   * when the actor stops, since at the time the actor existed. But afterwards(ActorRef might remain), if the watcher
   * calls watch(), then in reply Terminated will be sent with flag set as "false".
   * 2. Second - whether this message has been synthesized by the system.
   *
   * Note - "Terminated" extends "AutoReceiveMessage" ao that Akka itself takes care that Terminated is not sent after
   * unwatch has been called. It also extends "PossiblyHarmful" depicting the message is harmful during handle.
   */

  /*
   * As soon as context.actorOf(<Props>, "name") is called the child gets registered to actor context and can be
   * retrieved by context.child("name") - line #45. After the child terminates, a "Terminated" message is sent back to
   * the parent and it is unregistered from context if DeathWatch is not used. If used, it is unregistered before
   * Terminated message is sent.
   *
   * Error Kernel Pattern:
   * 1. Keep important data(or state) near the root. It is to avoid changing the important states as rarely as possible.
   * 2. Delegate the risky tasks to leaves. If exception happens at any such task and it is required to Restart the node,
   *    then it is better to restart the leaf than the upper nodes as restarting the upper nodes will effect its child
   *    nodes and their states. It could be possible that the current state of the children might be very important for
   *    the system functioning.
   */

  /*
   * EventStream - allows publication of messages to unknown audience(or ActorRefs). Every actor can optionally
   * subscribe to it using "context" and all actors have the ability to listen to these at certain channels.
   *
   * trait EventStream {
   *    def subscribe(subscriber: ActorRef, topic: Class[_]): Boolean
   *    def unsubscribe(subscriber: ActorRef, topic: Class[_]): Boolean
   *    def unsubscribe(subscriber: ActorRef): Unit
   *    def publish(event: AnyRef): Unit
   * }
   */
  class Listener extends Actor {
    context.system.eventStream.subscribe(self, classOf[LogEvent])
    def receive: Receive = {
      case e: LogEvent => ???
    }

    override def postStop(): Unit = context.system.eventStream.unsubscribe(self)
  }

  /*
   * What happens to unhandled messages ? - The messages that don't match the case specified goes to "unhandled" method
   * defined in Actor trait
   *
   * trait Actor {
   *  ...
   *  def unhandled(message: Any): Unit = message match {
   *    case Terminated(target) => throw new DeathPactException(target)
   *    case msg => context.event.eventStream.publish(UnhandledMessage(msg, sender, self))
   *  }
   * }
   *
   * On DeathPactException(), the default supervisor strategy handles it with a "Stop" command
   */

  /*
   * Persisting Actor State - Required to keep the system continue further from the point in incidents like power outage.
   * Since system switches off this way, the actor's last state is lost. 2 possibilities to persist state:
   * i. Mirror a storage(file or db) and perform in-place updates of both. So when actor's state changes the storage is also updated.
   * ii. Persisting changes than the state itself in append only fashion i.e. a record once persisted is never deleted
   * only added to. The current state can be derived by reapplying all the changes from beginning.
   *
   * Strategy (i) vs (ii):
   * 1. (i) uses less storage.
   * 2. (i) requires to access just 1 memory location to fetch last persisted state.
   * 3. (ii) lets us replay history i.e. any point can be taken and state can be restored doing all the changes till the point.
   * 4. (ii) performs multiple processing, hence errors caused can be corrected proactively.
   * 5. (ii) tracing the historical changes let's us gain insights to business that may result to profit.
   * 6. (ii) writing in append only stream optimizes IO bandwidth.
   *
   * Setting an amount of recovery an Actor can take, it is better to combine both the approaches i.e (ii) in logs and
   * snapshots depicting a state at regular intervals. Snapshots are recorded by Actor and are immutable in nature and
   * hence can be shared across. On recovery, the latest snapshot will be taken and all events after that will be
   * considered.
   */

  /*
   * How Actors actually persist changes?
   * i. Command-Sourcing i.e. to persist command to logs before processing it. The reply to the process can be sent normally.
   *    During recovery all logged commands are processed to reach the current state, but reply(that was meant to a particular actor)
   *    for every command is send to a channel that decides if the particular message has been previously processed.
   *    All side effects are kept local.
   * ii. Event-Sourcing i.e. persisting events(not states) that have happened once an Actor gets a command. This way a command represents
   *    something which is expected to happen while an event that has already happened. Hence during recovery an Actor
   *    doesn't have to process the commands but has to receive the events it generated and continue processing commands as
   *    usual once reaching the last state. Actually the events are applied to the current state to produce the next state.
   *
   * While going (i), an Actor may receive a new command between logging the old command and processing it. This way the
   * Actor still holds the previous state and so the same state will be reflected while processing the new state, hence
   * will act accordingly. So when to apply the events ?
   * i. After the stale state the Actor is in i.e. after the Actor has processed the message from log.
   * ii. Before the Actor's stale state.
   * iii. Not to process any new messages while waiting for persisted event to arrive back to Actor, rather keep them
   *      buffering. This degrades Actor's performance but retains consistency and resilience. Such ability to buffer
   *      the new messages concurrently is provided by "Stash" trait in Akka.
   */
  object UserProcessor {
    case class NewPost(text: String)
  }

  class UserProcessor extends Actor with Stash {

    import UserProcessor._
    // This Object is case class State
    var state: Object = ???
    def receive: Receive = {
      case NewPost(text) =>
        // Proceed when new post comes in -> change state to waiting for next 2 messages to be persisted.
        context.become(waiting(2), discardOld = false)
    }

    def waiting(n: Int): Receive = {
      // This Object is Event
      case e: Object =>
        state = state.updated(e)
        if (n==1) {context.unbecome(); unstashAll()}
        else context.become(waiting(n-1))
      case _ => stash()
    }
  }

}
