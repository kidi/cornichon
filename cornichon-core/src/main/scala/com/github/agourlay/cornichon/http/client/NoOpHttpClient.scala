package com.github.agourlay.cornichon.http.client

import cats.Show
import cats.data.EitherT
import cats.syntax.either._
import com.github.agourlay.cornichon.core.{ CornichonError, Done }
import com.github.agourlay.cornichon.http.{ HttpIsoStringResponse, HttpResponse, HttpIsoStringRequest, HttpRequest, HttpStreamedRequest }
import com.github.agourlay.cornichon.resolver.Resolvable
import monix.eval.Task
import org.http4s.{ EntityDecoder, EntityEncoder }

import scala.concurrent.duration.FiniteDuration

class NoOpHttpClient extends HttpClient {

  def runRequest[A: Show](cReq: HttpRequest[A], t: FiniteDuration)(implicit ee: EntityEncoder[Task, A]) =
    EitherT.apply(Task.now(HttpResponse(200, Nil, "NoOpBody").asRight))

  def runIsoStringRequest[A: Show, B: Show: Resolvable](cReq: HttpIsoStringRequest[A, B], t: FiniteDuration)(implicit ee: EntityEncoder[Task, A], ed: EntityDecoder[Task, B]): EitherT[Task, CornichonError, HttpIsoStringResponse[B]] =
    EitherT.apply(Task.now(HttpIsoStringResponse[B](200, Nil, None).asRight))

  def openStream(req: HttpStreamedRequest, t: FiniteDuration) =
    Task.now(HttpResponse(200, Nil, "NoOpBody").asRight)

  def shutdown() =
    Done.taskDone

  def paramsFromUrl(url: String) =
    Right(Nil)
}