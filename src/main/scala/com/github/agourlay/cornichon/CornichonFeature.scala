package com.github.agourlay.cornichon

import java.util.Timer
import java.util.concurrent.{ Executors, ThreadFactory }

import akka.stream.ActorMaterializer
import com.github.agourlay.cornichon.core._
import com.github.agourlay.cornichon.dsl.Dsl
import com.github.agourlay.cornichon.http.client.HttpClient
import com.github.agourlay.cornichon.http.{ HttpDsl, HttpService }
import com.github.agourlay.cornichon.json.JsonDsl
import com.github.agourlay.cornichon.resolver.{ Mapper, Resolver }
import com.github.agourlay.cornichon.CornichonFeature._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

trait CornichonFeature extends HttpDsl with JsonDsl with Dsl with ScalatestIntegration {

  protected var beforeFeature: Seq[() ⇒ Unit] = Nil
  protected var afterFeature: Seq[() ⇒ Unit] = Nil

  protected var beforeEachScenario: Seq[Step] = Nil
  protected var afterEachScenario: Seq[Step] = Nil

  implicit lazy val (globalClient, ec, _, _, timer) = globalRuntime
  private lazy val engine = Engine.withStepTitleResolver(resolver, ec)

  private lazy val config = ConfigFactory.load().as[Config]("cornichon")
  lazy val requestTimeout = config.requestTimeout
  lazy val baseUrl = config.baseUrl
  lazy val executeScenariosInParallel = config.executeScenariosInParallel

  lazy val http = httpServiceByURL(baseUrl, requestTimeout)
  lazy val resolver = new Resolver(registerExtractors)

  protected def registerFeature() = reserveGlobalRuntime()

  protected def unregisterFeature() = releaseGlobalRuntime()

  protected def runScenario(s: Scenario) = {
    println(s"Starting scenario '${s.name}'")
    engine.runScenario(Session.newEmpty, afterEachScenario.toList) {
      s.copy(steps = beforeEachScenario.toList ++ s.steps)
    }
  }

  def httpServiceByURL(baseUrl: String, timeout: FiniteDuration = requestTimeout) =
    new HttpService(baseUrl, timeout, globalClient, resolver)

  def feature: FeatureDef

  def registerExtractors: Map[String, Mapper] = Map.empty

  def beforeFeature(before: ⇒ Unit): Unit =
    beforeFeature = beforeFeature :+ (() ⇒ before)

  def afterFeature(after: ⇒ Unit): Unit =
    afterFeature = (() ⇒ after) +: afterFeature

  def beforeEachScenario(steps: Step*): Unit =
    beforeEachScenario = beforeEachScenario ++ steps

  def afterEachScenario(steps: Step*): Unit =
    afterEachScenario = steps ++ afterEachScenario
}

// Protect and free resources
private object CornichonFeature {

  import akka.actor.ActorSystem
  import scala.concurrent.duration._
  import java.util.concurrent.atomic.AtomicInteger
  import com.github.agourlay.cornichon.http.client.AkkaHttpClient

  implicit private lazy val system = ActorSystem("cornichon-actor-system")
  implicit private lazy val mat = ActorMaterializer()

  implicit private lazy val timer = new Timer("cornichon-timer")
  implicit private lazy val ec = ExecutionContext.fromExecutorService(
    Executors.newFixedThreadPool(
      Runtime.getRuntime.availableProcessors() + 1,
      new ThreadFactory {
        val count = new AtomicInteger(0)
        override def newThread(r: Runnable) = {
          new Thread(r, "cornichon-" + count.incrementAndGet)
        }
      }
    )
  )

  private lazy val client: HttpClient = new AkkaHttpClient()

  private val registeredUsage = new AtomicInteger
  private val safePassInRow = new AtomicInteger

  // Custom Reaper process for the time being
  // Will tear down stuff if no Feature registers during 10 secs
  system.scheduler.schedule(5.seconds, 5.seconds) {
    if (registeredUsage.get() == 0) {
      safePassInRow.incrementAndGet()
      if (safePassInRow.get() == 2) {
        client.shutdown().map { _ ⇒
          timer.cancel()
          timer.purge()
          ec.shutdown()
          mat.shutdown()
          system.terminate()
        }
      }
    } else if (safePassInRow.get() > 0) safePassInRow.decrementAndGet()
  }

  lazy val globalRuntime = (client, ec, system, mat, timer)
  def reserveGlobalRuntime(): Unit = registeredUsage.incrementAndGet()
  def releaseGlobalRuntime(): Unit = registeredUsage.decrementAndGet()
}
