package com.github.agourlay.cornichon.framework.examples.binary

import com.github.agourlay.cornichon.{ CornichonFeature, CornichonHttpFeature }
import com.github.agourlay.cornichon.framework.examples.HttpServer
import com.github.agourlay.cornichon.framework.examples.binary.server.BinaryEndpointApi
import com.github.agourlay.cornichon.framework.examples.superHeroes.server.SuperHeroesHttpAPI

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class BinaryEndpointScenario extends CornichonFeature {
  def feature =
    Feature("Cornichon feature example") {

      Scenario("demonstrate non json features") {

        When I getRaw[String, String]("/binconvert/Batman")

        //        Then I wait(1.hour)

        Then assert status.is(200)

        And I show_session
        And I show_last_status
        And I show_last_body
        And I show_last_headers
      }
    }

  lazy val port = 8080

  // Base url used for all HTTP steps
  override lazy val baseUrl = s"http://localhost:$port"

  //Travis CI struggles with default value `2.seconds`
  override lazy val requestTimeout = 5.second

  var server: HttpServer = _

  // Starts up test server
  beforeFeature {
    server = Await.result(new BinaryEndpointApi().start(port), 5.second)
    println(s"Server is Up ${server}")
  }

  // Stops test server
  afterFeature {
    Await.result(server.shutdown(), 5.second)
  }
}