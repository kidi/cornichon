package com.github.agourlay.cornichon.framework.examples.binary.server

import com.github.agourlay.cornichon.framework.examples.HttpServer
import monix.eval.Task
import monix.execution.{ CancelableFuture, Scheduler }
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.GZip

class BinaryEndpointApi() extends Http4sDsl[Task] {
  implicit val s = Scheduler.Implicits.global

  object SessionIdQueryParamMatcher extends QueryParamDecoderMatcher[String]("sessionId")
  object ProtectIdentityQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Boolean]("protectIdentity")
  object JustNameQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Boolean]("justName")

  private val superHeroesService = HttpRoutes.of[Task] {

    case GET -> Root / "binconvert" / name =>
      Ok(s"call a superheros here with $name")

  }

  private val routes = Router(
    "/" -> superHeroesService
  )

  def start(httpPort: Int): CancelableFuture[HttpServer] =
    BlazeServerBuilder[Task](executionContext = s)
      .bindHttp(httpPort, "localhost")
      .withoutBanner
      .withNio2(true)
      .withHttpApp(GZip(routes.orNotFound))
      .allocated
      .map { case (_, stop) => new HttpServer(stop) }
      .runToFuture
}
