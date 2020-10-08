package com.github.agourlay.cornichon.http.client

import cats.Show
import cats.data.EitherT
import com.github.agourlay.cornichon.core.{ CornichonError, Done }
import com.github.agourlay.cornichon.http.{ HttpIsoStringResponse, HttpResponse, HttpIsoStringRequest, HttpRequest, HttpStreamedRequest }
import com.github.agourlay.cornichon.resolver.Resolvable
import monix.eval.Task
import org.http4s.{ EntityDecoder, EntityEncoder }

import scala.concurrent.duration.FiniteDuration

trait HttpClient {

  def runRequest[A: Show](cReq: HttpRequest[A], t: FiniteDuration)(implicit ee: EntityEncoder[Task, A]): EitherT[Task, CornichonError, HttpResponse]
  def runIsoStringRequest[A: Show, B: Show: Resolvable](cReq: HttpIsoStringRequest[A, B], t: FiniteDuration)(implicit ee: EntityEncoder[Task, A], ed: EntityDecoder[Task, B]): EitherT[Task, CornichonError, HttpIsoStringResponse[B]]

  def openStream(req: HttpStreamedRequest, t: FiniteDuration): Task[Either[CornichonError, HttpResponse]]

  def shutdown(): Task[Done]

  def paramsFromUrl(url: String): Either[CornichonError, Seq[(String, String)]]
}