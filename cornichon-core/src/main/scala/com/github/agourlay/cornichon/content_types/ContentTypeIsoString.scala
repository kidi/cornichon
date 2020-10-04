package com.github.agourlay.cornichon.content_types

import cats.Show
import com.github.agourlay.cornichon.core.CornichonError
import io.circe.{ Decoder, DecodingFailure, Encoder, Json }
import cats.syntax.either._
import com.github.agourlay.cornichon.content_types.ContentTypeIso.{ Parsable, asParsed }
import com.github.agourlay.cornichon.json.JsonDecodingFailure

object ContentTypeIso {
  type Parsable[A] = Either[CornichonError, A]

  def asCornichonError[T](ce: CornichonError): Parsable[T] = Either.left[CornichonError, T](ce)
  def asCornichonError[T](t: Throwable): Parsable[T] = asCornichonError(CornichonError.fromThrowable(t))
  def asParsed[T](t: T): Parsable[T] = Either.right[CornichonError, T](t)

  def buildFromCirce[A: Show: Encoder: Decoder]: ContentTypeIso[A, Json] = new ContentTypeIso[A, Json] {
    def from(a: A): Parsable[Json] = {
      val encoder = implicitly[Encoder[A]]
      asParsed[Json](encoder(a))
    }

    def to(b: Json): Parsable[A] = {
      val decoder = implicitly[Decoder[A]]
      decoder.decodeJson(b).fold[Parsable[A]]((e: DecodingFailure) => asCornichonError[A](JsonDecodingFailure(b, e.message)), asParsed[A])
    }
  }
}

trait ContentTypeIso[A, B] {
  def from(a: A): ContentTypeIso.Parsable[B]
  def to(b: B): ContentTypeIso.Parsable[A]
}

object ContentTypeIsoString {
  implicit def identity: ContentTypeIsoString[String] = new ContentTypeIsoString[String] {
    def from(a: String): Parsable[String] = asParsed[String](a)

    def to(b: String): Parsable[String] = asParsed[String](b)
  }
}

trait ContentTypeIsoString[A] extends ContentTypeIso[String, A]
