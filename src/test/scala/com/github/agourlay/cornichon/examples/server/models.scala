package com.github.agourlay.cornichon.examples.server

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import de.heikoseeberger.akkasse.ServerSentEvent
import spray.json.DefaultJsonProtocol

import scala.collection.mutable
import scala.concurrent._
import scala.util.Random

case class Publisher(name: String, foundationYear: Int, location: String)

case class SuperHero(name: String, realName: String, city: String, hasSuperpowers: Boolean, publisher: Publisher)

class TestData(implicit executionContext: ExecutionContext) {

  def reset() = Future {
    publishers.clear()
    publishers.++=(initialPublishers)
    superHeroes.clear()
    superHeroes.++=(initialSuperheroes)
  }

  def publisherByName(name: String) = Future {
    publishers.find(_.name == name).fold(throw new PublisherNotFound(name)) { c ⇒ c }
  }

  def addPublisher(p: Publisher) = Future {
    if (publishers.exists(_.name == p.name)) throw new PublisherAlreadyExists(p.name)
    else {
      publishers.+=(p)
      p
    }
  }

  def updateSuperhero(s: SuperHero) =
    for {
      _ ← superheroByName(s.name)
      _ ← publisherByName(s.publisher.name)
      _ ← deleteSuperhero(s.name)
      updated ← addSuperhero(s)
    } yield updated

  def addSuperhero(s: SuperHero) =
    publisherByName(s.publisher.name).map { _ ⇒
      if (superHeroes.exists(_.name == s.name)) throw new SuperHeroAlreadyExists(s.name)
      else {
        superHeroes.+=(s)
        s
      }
    }

  def deleteSuperhero(name: String) =
    superheroByName(name).map { sh ⇒
      superHeroes.-=(sh)
      sh
    }

  def superheroByName(name: String, protectIdentity: Boolean = false) = Future {
    val sh = {
      if (name == "random") Some(randomSuperhero)
      else superHeroes.find(_.name == name)
    }
    sh.fold(throw new SuperHeroNotFound(name)) { c ⇒
      if (protectIdentity) c.copy(realName = "XXXXX")
      else c
    }
  }

  def allPublishers = Future { publishers.toSeq }

  def allSuperheroes = Future { superHeroes.toSeq }

  def randomSuperhero: SuperHero =
    Random.shuffle(superHeroes).head

  private val initialPublishers = Seq(
    Publisher("DC", 1934, "Burbank, California"),
    Publisher("Marvel", 1939, "135 W. 50th Street, New York City")
  )

  val publishers = mutable.ListBuffer.empty[Publisher].++=(initialPublishers)

  private val initialSuperheroes = Seq(
    SuperHero("Batman", "Bruce Wayne", "Gotham city", hasSuperpowers = false, publishers.head),
    SuperHero("Superman", "Clark Kent", "Metropolis", hasSuperpowers = true, publishers.head),
    SuperHero("GreenLantern", "Hal Jordan", "Coast City", hasSuperpowers = true, publishers.head),
    SuperHero("Spiderman", "Peter Parker", "New York", hasSuperpowers = true, publishers.tail.head),
    SuperHero("IronMan", "Tony Stark", "New York", hasSuperpowers = false, publishers.tail.head)
  )

  val superHeroes = mutable.ListBuffer.empty[SuperHero].++=(initialSuperheroes)

}

trait ResourceNotFound extends Exception {
  val id: String
}
case class PublisherNotFound(id: String) extends ResourceNotFound
case class SuperHeroNotFound(id: String) extends ResourceNotFound

trait ResourceAlreadyExists extends Exception {
  val id: String
}
case class PublisherAlreadyExists(id: String) extends ResourceNotFound
case class SuperHeroAlreadyExists(id: String) extends ResourceNotFound

case class HttpError(error: String)

trait JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val formatCP = jsonFormat3(Publisher)
  implicit val formatSH = jsonFormat5(SuperHero)
  implicit val formatHE = jsonFormat1(HttpError)
  implicit def toServerSentEvent(sh: SuperHero): ServerSentEvent = {
    ServerSentEvent(eventType = "superhero", data = formatSH.write(sh).compactPrint)
  }
}