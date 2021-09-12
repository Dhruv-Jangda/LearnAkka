package com.learnakka

import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, ReceiveTimeout, Status, SupervisorStrategy, Terminated}
import akka.pattern.pipe
import com.learnakka.BankAccountApp.BankAccount.Done
import com.learnakka.LinkCheckerApp.Getter.Abort
import com.ning.http.client.AsyncHttpClient
import org.jsoup.Jsoup

import java.util.concurrent.Executor
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, Promise}
import scala.jdk.CollectionConverters.IteratorHasAsScala

object LinkCheckerApp extends App {

  /*
   * While designing a system in Akka actors:
   * 1. Think of a room full of capable people.
   * 2. Divide a given task to further subtasks till the unit level.
   * 3. Assign the subtasks(any level) to the Actors. (Observe that we'll be forming up a tree with different Actors
   * acting as Persons with different responsibilities)
   * 4. Draw a diagram with communication lines of how tasks will be split up.
   */

  /*
   * Given a URL, write an Actor system that will recursively download content, extract links and follow them, bounded
   * by a maximum depth. All links encountered have to be returned.
   *
   * Can be modelled as recursive action till depth reached as:
   *
   * {client} <(m2)--(m1)> [receptionist] <(m4)--(m3)> [controller] <(m6)--(m5)> [getter_i]
   *
   * i. where i from 1 to n
   * ii. (m3)> means message send to [controller]
   * iii. <(m4) means message received back from [controller]
   *
   * [receptionist] - responsible for getting incoming requests from {client}
   * [controller] - responsible for
   *    a. getting urls from [receptionist]
   *    b. checking of the urls if visited previously else recursion will run endlessly.
   *    c. sending url with depth(d) to getter.
   *    d. after receiving (m6) spawn other such [getter_i] for further link scrapping + url sending and checking can
   *       be done in parallel. Hence states of the actors can have depth as a parameter so that it is not required to
   *       remember the depth at controller. Moreover working in parallel, it would be difficult to store 'd' for each [getter_i]
   *       [getter_i] - responsible for searching the url and responding back the url found further with (depth-1)
   *
   * (m1) - get(url)
   * (m2)* - result(url,links)
   * (m3) - check(url,d)
   * (m4)* - result(links)
   * (m5) - get(url)
   * (m6)* - (multiple links, msg - Done). * - send back
   */

  /*
   * Plan of Action
   * 1. Write web client that turns a URL to HTTP body asynchronously. "com.ning" % "async-http-client" % "1.7.19"
   * 2. Write [getter_i] to process the body.
   * 3. Write [controller] which spawns [getter_i] for all links encountered.
   * 4. Write [receptionist] to manage one [controller] per request.
   */

  val extClient = new AsyncHttpClient()

  def get(url: String): String = {
    val response = extClient.prepareGet(url).execute().get()
    if (response.getStatusCode < 400)
      response.getResponseBodyExcerpt(128 * 1024) // 128 kb
    else
      throw new Exception(s"${response.getStatusCode}")
  }

  /*
   * Note: .execute() outputs Future but .get() waits till the Future is complete => Blocks the Actor from replying to
   * other messages since it works in an asynchronous fashion. It also is wasting one thread, since Actor execution
   * is single threaded. Thread being a finite resource should not be wasted.
   */

  trait WebClient {
    def getAsync(url: String)(implicit exec: Executor): Future[String]
  }

  /*
    https://stackoverflow.com/questions/2190161/difference-between-java-lang-runtimeexception-and-java-lang-exception
    java.lang.RunTimeException is a category of un-checked exceptions:
    * Can be prevented programmatically. Eg NullPointerException, ArrayIndexOutOfBoundException, etc.
    * The calling method might not need to handle it specifically.
    java.lang.Exception is a category of checked exceptions:
    * Can't be prevented programmatically.
    * The calling method has to handle it in try-catch block specifically.
   */
  case class BadStatus(status: Int) extends RuntimeException

  object AsyncWebClient extends WebClient {
    val client = new AsyncHttpClient()

    // To create a Non-Blocking url handler, Scala's Promise-Future can be used
    def getAsync(url: String)(implicit exec: Executor): Future[String] = {
      val f = client.prepareGet(url).execute()
      val p = Promise[String]()

      /*
       * Just like scala Future.onComplete(<func>) executes <func> on future's completion, f.addListener works the same
       * way. Now since we know the block inside .addListener() works after the future f is completed, it'll now be
       * executed in another thread so the control will move further and f.get() becomes non-blocking.
       */
      f.addListener(new Runnable {
        def run = {
          val response = f.get()
          if (response.getStatusCode < 400)
            p.success(response.getResponseBodyExcerpt(128 * 1024))
          else
            p.failure(BadStatus(response.getStatusCode))
        }
      }, exec)
      p.future
    }

    def shutdown(): Unit = client.close()

  }

  /*
   * Hence a reactive application should always be:
   * i. non-blocking, since any blocking event in a reactive application affects other parts of the code
   * ii. event-driven top to bottom.
   */

  object Getter {
    case object Done
    case object Abort
  }
  // Using these we can create our Getter
  class Getter(url: String, depth: Int) extends Actor {

    // This dispatcher is implicitly required in Actors, potentially shared and can run Futures as well.
    implicit val exec = context.dispatcher.asInstanceOf[Executor with ExecutionContext]
    def client: WebClient = AsyncWebClient

    client getAsync url pipeTo self // Writing in English format

    /*
     * Above line expands to:
     * val future = WebClient.get(url)
     * .get() here requires Executor that is declared implicitly hence picked up automatically
     * Iteratively sending the body on Success to itself till the depth = 0
     * future.pipeTo(self)
     * .pipeTo requires ExecutionContext(implicitly available within Akka) to run Scala Future.
     * Due to these the code block will run concurrently
     */

    /*
     * future.pipeTo(self) is syntactic sugar to below code block. .pipeTo is a method of Akka & included since
     * the below pattern of code block is very common in Akka
     * future onComplete {
     * case Success(body) => self ! body
     * case Failure(err) => self ! Status.Failure(err)
     * }
     */
    def receive: PartialFunction[Any, Unit] = {
      case body: String =>
        for (link <- findLinks(body,url)) {
          // Every Actor is created by an Actor which acts as a Parent & .parent called returns its ActorRef
          context.parent ! Controller.Check(link, depth)
        }
        stop()
      case Abort => stop()
      case _ : Status.Failure => stop()
    }

    // Finding Links by parsing Http body and extracting href - "<a href=""></a>" from the body using "org.jsoup" artifact
    def findLinks(body: String, url: String): Iterator[String] = {
      val document = Jsoup.parse(body, url)
      val links = document.select("a[href]") // Similar to BeautifulSoup in python
      for {
        link <- links.iterator().asScala
      } yield link.absUrl("href")
    }

    def stop(): Unit = {
      context.parent ! Done
      context.stop(self)
    }
  }

  /*
   * Now, we'll create an Actor that logs progress made but logging involves IO operations that are blocking in nature.
   * Hence in case of logging in Actors, Akka's logging system can be utilized since logging is passed as a task to
   * dedicated Actors. Now Actors for logging came to picture making the system non-blocking. Can set level using
   * akka.loglevel = INFO/DEBUG/WARNING/ERROR. ActorLogging class returns a log function to log messages. The source
   * info returned from ActorLogging will have the Actor's name, hence the Actor has to named properly.
   */
  class A extends Actor with ActorLogging {
    def receive: Receive = {
      case msg => log.debug("received message: {}",msg)
    }
  }

  object Controller {
    case class Check(url: String, depth: Int)
    case class Result(cache: Set[String])
  }

  class Controller extends Actor with ActorLogging {
    import Controller._
    override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5) {
      case _ : Exception => SupervisorStrategy.Restart
    }

    /*
     * If a server url responds very slowly, then AsyncWebClient.getAsync(url) will return a Failure, however checking
     * the same url manually might not give an issue as the url is still working though responding slowly. Hence we need
     * to wait for sometime unless we get back a message. context provides this functionality and after getting each
     * message the timer resets back to specified wait time.
     */
    context.setReceiveTimeout(10.seconds)

    /*
     * Best practise involves using Immutable Data Structures in an Actor system, since it will confuse another Actor(
     * assuming it is also modifying the same variable) if one of the parent modifies the same mutable variable.
     * Here Set is used as an immutable.
     */

    var cache = Set.empty[String]
    // var children = Set.empty[ActorRef] <- Old
    def receive: Receive = {
      case Check(url, depth: Int) =>
        log.debug("{} checking {}", depth, url)
        if (!cache.contains(url) && depth > 0)
          // children += context.actorOf(Props(new Getter(url, depth-1))) <- Old
          context.watch(context.actorOf(Props(new Getter(url, depth-1)))) // <- no name provided so default Akka names it as "$_"
        cache += url
      /* Old
        case Getter.Done =>
        children -= sender
        if (children.isEmpty) {
          context.parent ! Result(cache)
        }
      */
      case Terminated(_) =>
        if(context.children.isEmpty) context.parent ! Result(cache)
      // Thrown when timeout as set by context.setReceiveTimeout()
      case ReceiveTimeout =>
        // children.foreach(_ ! Getter.Abort) <- Old
        context.children.foreach(context.stop)
    }
  }

  /*
   * Actor includes timer service optimized for high volume, short durations and frequent cancellation via trait "Scheduler"
   * trait Scheduler {
   *  def scheduleOnce(delay: FiniteDuration, target: ActorRef, msg: Any)(implicit ec: ExecutionContext): Cancellable
   *  def scheduleOnce(delay: FiniteDuration)(block => Unit)(implicit ec: ExecutionContext): Cancellable
   *  ...
   * }
   * Shown are definitions for one time executions. Similar executions are there for repeated scheduling operations.
   * Actually used to send messages to an Actor at a scheduled future time.
   *
   * To handle timeout in a different fashion i.e. timing out after Controller initiated rather than a message
   * received then the scheduler will used. "context" in Actor gives access to "system" i.e. the container in which all
   * Actors run which further gives access to the "scheduler".
   *
   * implicit val ec = context.dispatcher
   * context.system.scheduler.scheduleOnce(10.seconds)({
   *   children.foreach(_ ! Getter.Abort)
   * })
   *
   * The issue here is that the block will be run by the "Scheduler" and not the Actor => it is out of thread of the
   * Actor or it is not thread safe. Now "children" is of type var which is shared between both the blocks => will be
   * modified by one of the blocks and can cause many issues. Hence the first variant of scheduleOnce is preferred.
   *
   * context.system.scheduler.scheduleOnce(10.seconds, self, ReceiveTimeout)
   * ... and handle case "ReceiveTimeout" as before(line #228) in receive. Here message is received back to Actor
   * hence thread safety is governed.
   *
   * NOTE:
   * Hence any object of type var if accessed outside of scope of the Actor has to be send as a message back to the
   * Actor and handled in "receive" <- Good design practice. Also do not refer to actor state directly from code
   * running asynchronously. If required, always cache the sender's state and then use that state than the actual
   * Actor itself.
   */

  object Receptionist {
    case class Get(url: String)
    case class Failed(url: String, reason: String)
    case class Result(url: String, links: Set[String])
  }

  class Receptionist extends Actor {
    import Receptionist._

    case class Job(client: ActorRef, url: String)
    var reqNo = 0

    def runNext(queue: Vector[Job]) : Receive = {
      reqNo += 1
      if (queue.isEmpty) waiting
      else {
        val controller = context.actorOf(Props[Controller,s"c$reqNo"])
        controller ! Controller.Check(queue.head.url, 2)
        running(queue)
      }
    }

    def enqueueJob(queue: Vector[Job], job: Job): Receive = {
      if (queue.size > 3) {
        sender ! Failed(job.url, "controller failed unexpectedly!")
        running(queue)
      }
      else
        running(queue :+ job)
    }

    def receive: Receive = waiting

    def waiting: Receive = {
      // On get(url) begin traversal and become running
      case Get(url) => context.become(runNext(Vector(Job(sender, url))))
    }

    def running(queue: Vector[Job]) : Receive = {
      /*
       * If get(url) while running, append url to queue and keep running. If Controller.Result(links), ship it to
       * the client and run next job from queue.
       */
      case Controller.Result(links) =>
        val job = queue.head
        job.client ! Result(job.url, links)
        context.stop(sender)
        context.become(runNext(queue.tail))

        /*
         * Good design tips -
         * 1. Use context.become(<state>) for different states with data local to the state's behaviour.
         *    Observe that in every state(or function defined) the data send is used/modified locally in that
         *    state(or function) and not here while context.become(<state>).
         * 2. Never refer to actor state from code running asynchronously.
          */

      case Get(url) =>
        context.become(enqueueJob(queue, Job(sender, url)))
    }
  }

  class Main extends Actor {
    import Receptionist._

    val receptionist = context.actorOf(Props[Receptionist], "receptionist")
    receptionist ! Get("https://www.google.com")

    implicit val ec = context.dispatcher
    context.system.scheduler.scheduleOnce(10.seconds, self, ReceiveTimeout)

    def receive: Receive = {
      case Result(url, links) => println(links.toVector.sorted mkString s"Results for $url - \n", "\n", "\n")
      case Failed(url, reason) => println(s"Failed to fetch results for $url, reason $reason")
      case ReceiveTimeout => context.stop(self)
    }

    // Event to gracefully close the connection and shedding the dependent resources when the main Actor stops itself
    override def postStop(): Unit = AsyncWebClient.shutdown()
  }
}
