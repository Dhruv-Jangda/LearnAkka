package com.learnakka

import akka.actor.{Actor, ActorIdentity, ActorPath, ActorRef, ActorSystem, Identify, Props}
import akka.cluster.{Cluster, ClusterEvent}

object DistributedComputing {

  class Greeter extends Actor {
    def receive: Receive = ???
  }

  class UserActor extends Actor {
    val system = ActorSystem("HelloWorld")
    def receive: Receive = ???

    val ref = system.actorOf(Props[Greeter], "greeter")
    /*
     * Actors are created just like a file system i.e. each of them corresponds to a specific URI(Uniform Resource
     * Identifier). Eg: Local address example: ref.path - akka://HelloWorld/user/greeter ~ <authority>/<path>, here
     * <path> = user/greeter. Remote address <authority>: akka.tcp://HelloWorld@10.2.4.6:6565 where 10.2.4.6 = remote
     * server ip and 6565 is its port no. at which teh Actor system can serve. Every Actor when created is given an id
     * and same is reflected in its ActorRef - user/greeter#43428597
     *
     * ActorPath is the full name of an actor whether an Actor exists or not.
     * ActorRef points to an actor which was started.
     * Hence for life cycle monitoring ActorRef is used since it is not able to tell if actor exists by using ActorPath
     */

    /*
     * When communicating over remote actors, it si necessary to:
     * 1. Talk to actors that are not yet created.
     * 2. And for which we don't have means to acquire ActorRef, though we know at which hosted port the actor lives.
     * To support this we use context.actorSelection(<ActorPath>) and send a message. Here we use Identify(<variable>),
     * an inbuilt akka message that every Actor handles. It sends back ActorIdentity(<variable>, <ActorRef>) with same
     * variable as used in Identify() and an <ActorRef> that sends it.
     */
    object Resolver {
      case class Resolve(path: ActorPath)
      case class Resolved(path: ActorPath, ref: ActorRef)
      case class NotResolved(path: ActorPath)
    }

    class Resolver extends Actor {
      import Resolver._
      def receive: Receive = {
        case Resolve(path) =>
          context.actorSelection(path) ! Identify((path, sender))
        case ActorIdentity((path: ActorPath, client: ActorRef), Some(ref)) =>
          client ! Resolved(path, ref)
        case ActorIdentity((path: ActorPath, client: ActorRef), None) =>
          client ! NotResolved(path)
      }
    }

    /*
     * Path passed to context.actorSelection(path) can be:
     * a. "child/grandchild" - Looking up for actor grandchild created by child.
     * b. "../grandchild" - Looking up for actor grandchild created by any actor.
     * c. "/child/grandchild" - Looking up for actor grandchild created by child from root.
     * d. "/child/grandchild/ *" - Looking up for all children of actor grandchild created by child from root.
     * context.actorSelection(path) returns an ActorName and sending message to an ActorName than ActorRef is provided
     * by Akka Cluster module("com.typesafe.akka" %% "akka-cluster" % "2.2.1").
     *
     * A Cluster is a set of nodes which all members are in agreement with each other i.e. alias to a group of people
     * that know each other very well and foreigners are treated external to the cluster. So all nodes of a cluster can
     * collaborate on a common task.
     * a. A single node can declare itself a cluster i.e cluster of size = 1.
     * b. A single node can join a cluster. Then a request can be sent to any member. Once all members know about the
     * new node, it is declared a part of the cluster. Information is spread via a Gossip Protocol(Resilient in nature).
     * c. An Akka cluster doesn't have any central leader that can act as a single point of Failure.
     *
     * In application.conf defined in class path, the cluster configuration is set as:
     * akka {
     *  actor {
     *    provider = akka.cluster.ClusterActorRefProvider
     *  }
     * }
     */

    class ClusterMain extends Actor {
      val cluster = Cluster(context.system)
      cluster.subscribe(self, classOf[ClusterEvent.MemberUp])
      cluster.join(cluster.selfAddress)

      def receive: Receive = {
        case ClusterEvent.MemberUp(member) =>
          if (member.address != cluster.selfAddress)
            ??? // Someone joined
      }
    }

    // This will start a single node cluster on port 2552
  }
}