package com.github.agourlay.cornichon

import com.github.agourlay.cornichon.dsl.{ BaseFeature, CoreDsl }
import com.github.agourlay.cornichon.http.{ HttpDsl, HttpJsonDsl }

trait CornichonBaseFeature extends BaseFeature with CoreDsl
trait CornichonHttpFeature extends CornichonBaseFeature with HttpDsl
trait CornichonFeature extends CornichonBaseFeature with HttpJsonDsl