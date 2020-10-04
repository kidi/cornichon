package com.github.agourlay.cornichon

import com.github.agourlay.cornichon.dsl.{ BaseFeature, CheckDsl, CoreDsl }
import com.github.agourlay.cornichon.http.{ HttpBaseDsl, HttpDsl }
import com.github.agourlay.cornichon.json.JsonDsl

trait CornichonBaseFeature extends BaseFeature with CoreDsl
trait CornichonJsonFeature extends CornichonBaseFeature with HttpDsl with JsonDsl with CheckDsl
trait CornichonHttpFeature extends CornichonBaseFeature with HttpBaseDsl with CheckDsl
trait CornichonFeature extends CornichonHttpFeature with CornichonJsonFeature