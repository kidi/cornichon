package com.github.agourlay.cornichon.http

import cats.Show
import com.github.agourlay.cornichon.resolver.Resolvable

case class HttpResponse(status: Int, headers: Seq[(String, String)] = Nil, body: String)
case class HttpIsoStringResponse[B: Show: Resolvable](status: Int, headers: Seq[(String, String)] = Nil, body: Option[B])