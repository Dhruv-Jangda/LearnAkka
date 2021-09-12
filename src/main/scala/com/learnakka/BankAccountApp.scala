package com.learnakka

import akka.actor.{Actor, ActorRef, Props}

object BankAccountApp {

  // It is a good practice to define an actor's messages in its companion object. Here case class represents messages to be used in Actor.
  object BankAccount {

    case class Deposit(amount: BigInt) {
      require(amount > 0) // require is an inbuilt Scala function that throws an error if condition is not met.
    }

    case class Withdraw(amount: BigInt) {
      require(amount > 0)
    }

    case object Done

    case object Failed

  }

  class BankAccount extends Actor {

    import BankAccount._

    var balance = BigInt(0)

    def receive: PartialFunction[Any, Unit] = {
      case Deposit(amount) =>
        balance += amount
        sender ! Done
      case Withdraw(amount) if (balance >= amount) =>
        balance -= amount
        sender ! Done
      case _ => sender ! Failed
    }
  }

  /*
  * Let's say an account(here Actor) A has to "transfer" money to other account B => Logic of "transfer" has to be
  * defined in the Actors. But it is actually not required for the account to know how the transfer has to take place.
  * Hence, we'll define another account(Actor) P that helps in "transfer" i.e. A --(withdraw)--> P then P -(done)-> A
  * and P --(deposit)--> B then B -(done)-> P. After transfer, P should terminate itself.
  * */
  object WireTransfer {

    case class Transfer(from: ActorRef, to: ActorRef, amount: BigInt)

    case object Done

    case object Failed

  }

  class WireTransfer extends Actor {

    import WireTransfer._

    def receive: PartialFunction[Any, Unit] = {
      case Transfer(from, to, amount) =>
        from ! BankAccount.Withdraw(amount)
        context.become(awaitWithdraw(to, amount, sender))
    }

    def awaitWithdraw(to: ActorRef, amount: BigInt, client: ActorRef): Receive = {
      case BankAccount.Done =>
        to ! BankAccount.Deposit(amount)
        context.become(awaitDeposit(client))

      case BankAccount.Failed =>
        client ! Failed
        context.stop(self) // In case of failure stop this transaction
    }

    def awaitDeposit(client: ActorRef): Receive = {
      case BankAccount.Done =>
        client ! Done
        context.stop(self)

      case BankAccount.Failed =>
        client ! Failed
        context.stop(self)
    }
  }

  // Main class to carry out the transaction
  class TransferMain extends Actor {
    val accountA = context.actorOf(Props[BankAccount], "accountA")
    val accountB = context.actorOf(Props[BankAccount], "accountB")

    // Deposit some money
    accountA ! BankAccount.Deposit(100)

    def receive: PartialFunction[Any, Unit] = {
      case BankAccount.Done => transfer(50)
    }

    def transfer(amount: BigInt) = {
      val transaction = context.actorOf(Props[WireTransfer], "transaction")
      transaction ! WireTransfer.Transfer(accountA, accountB, amount)
      context.become {
        case WireTransfer.Done =>
          print("Transaction Successful!")
          context.stop(self)

        case WireTransfer.Failed =>
          print("Transaction Failed")
          context.stop(self)
      }
    }
  }

  /*
   * A basic problem with message communication is that it is inherently unreliable i.e. if any issue(critical exception,
   * hardware failure, etc) happens during communication then the message won't be delivered successfully. Hence a successful
   * message transfer requires a "channel" and "acknowledgement" from the recipient. The above WireTransfer actor suffers
   * from the same issue. To counter this, Akka provide resending of message and the acknowledgement several times as specified:
   * 1. "at-most-once" - sending once delivers [0,1] times
   * 2. "at-least-once" - resending until acknowledged delivers [1,infinity) times
   * 3. "exactly-once" - processing only first reception delivers 1 time
   *
   * If an actor is passing messages to another actor on a different server that powers down due to power outage.
   * When power comes back the communication can continue from where it was not successful. This is how it helps
   *
   * Also, the communication can be unreliable but messages:
   * 1. Can be persisted(stored in a local storage) - so that can be used at a later time of communication hamper.
   * 2. Can include unique correlation ids - to keep the message unique.
   * 3. Delivery can be retied until Successful.
   *
   * Adding all these to capabilities to WireTransfer Actor:
   * 1. Log activities of WireTransfer to persistent storage
   * 2. Each transfer has unique ID.
   * 3. Add ID to Withdraw & Deposit.
   * 4. Store IDs of completed actions within BankAccount.
   * */

  /*
   * Message Ordering - If an actor send multiple messages to the same destination, they will arrive in the same exact order
   */
}
