package com.learnakka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object StreamProcessing {
  /**
   * There is data in such a huge amount that all can't be fit to a memory at once. Hence a pipeline with related
   * operations is created and a sample of the data is taken to apply to the pipeline to get the desired output. This way
   * all batches of data are processed. The way the tiny batch of data is put to memory to be used in the pipeline is
   * Streaming. Hence streaming processing aims to address:
   * i. Compositional building blocks.
   * ii. Flow control through such stream pipeline.
   * iii. Process infinite elements at an optimal rate.
   */

  /**
   * Back pressure or Flow control - mechanism to control the rate at which an upstreams publisher is emitting data to a
   * downstream subscriber in proportion to subscriber's receiving capabilities. Eg: pushing items to a shared queue
   * asynchronously and pulling the items from the queue such that no operation(push/pull) halting i.e. the rate of pushing
   * >= the rate of pulling. But if much greater, then the queue above will start to overflow or OOM error. So:
   * i. Upstream produces data at "fast rate".
   * ii. Downstream consumes data at "slow rate".
   *
   * This issue is handled by "reactive streams" that provides a standard for async stream processing with non blocking
   * back-pressure
   */

  /**
   * Reactive streams define:
   * i. A set of interaction rules(the "Specification")
   * ii. A set of Types(the SPI)
   * iii. a Technology Compliance(test) Kit (TCK) that tests the rules in (i)
   */

  implicit val system = ActorSystem()
  // ActorMaterializer takes ActorSystem as implicitly and transforms an streaming pipeline to an executioner
  implicit val mat = ActorMaterializer()

  val eventuallyResult : Future[Int] = Source(1 to 10).map(_ * 2).runFold(0)((acc, x) => acc + x)
  /**
   * .runFold is an operation that starts the streaming and  is a syntactic sugar for:
   * .runWith(
   *    Sink.fold(0)((acc: Int, x: Int) => acc + x)
   * )
   * Note - The .map operation used on the Source is different from .map of collections in Scala but has been built to
   * use in exactly the same way as in Scala collections.
   */
  val numbers = Source(1 to 10)
  val doubling = Flow.fromFunction((x: Int) => x * 2)
  val sum = Sink.fold(0)((acc: Int, x: Int) => acc + x)

  /**
   * Actually, the Source, Flow and Sink work as:
   * Source[Square] >--(.via)--> Flow[Square, Triangle] >--(.to)--> Sink[Triangle]
   * Materialization = Sources, Flows & Sinks as description of "what needs to be done". During materialization this
   * description is passed to an execution engine i.e. ActorMaterializer in Actor's case
   * Source[Triangle] >--> Sink[Square]
   */
  val eventuallyResultNew : Future[Int] = numbers.via(doubling).runWith(sum)

  /**
   * In Akka streams, steps of the processing pipeline("Graph") are called "Stages". A "stage" also covers fan -in/out
   * shapes. Term "operator" is used for DSL's API such as .map, .filter. Shapes in Akka are:
   * i. Source - has 1 output.
   * ii. Flow - has 1 input and 1 output.
   * iii. Sink- has 1 input.
   *
   * The raw SPI behind the Source, Flow and Sink:
   * a. Sink[I, _] (AS) ~ Subscriber[I] (RS), where I(input), AS(Async Streams) and RS(Reactive Streams) i.e.
   *    Subscriber subscribes to get 'streams' from the Flow
   * b. Flow[I, O, _] (AS) ~ Processor[I, O] (RS)
   * c. Source[O, _] (AS) ~ Publisher[O] (RS) i.e. the Publisher publishes 'streams' to the Flow
   *
   *
   *
   */




}
