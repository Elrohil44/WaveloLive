package Bikes

import Bikes.{UpdateAll, Updating}
import Database.BikeDatabase
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by Wiesiek on 2017-05-20.
  */

class BikesUpdator(val bikes: Bikes, implicit val system: ActorSystem, implicit val materializer: ActorMaterializer)
  extends Actor with ActorLogging with JsonSupport{
  import akka.pattern.pipe
  import context.dispatcher

  val http = Http(system)
  var allUpdated: Boolean = true
  private val db = BikeDatabase

  def update(): Unit = {
    http.singleRequest(HttpRequest(uri = Bikes.url)).pipeTo(self)
  }

  def receive = {
    case Updating => update()
    case UpdateAll =>
      allUpdated = false
      update()
    case HttpResponse(StatusCodes.OK, _, entity, _) =>
      Unmarshal(entity).to[String]
        .onComplete({
          case Success(json) =>
            val _list = json.parseJson.convertTo[BikeList].items

            // I don't know if it is very effective but it works
            // It adds bikes that were rented and haven't been collected by the server since
            // the server started
            val available = (for (item <- _list) yield new Bike(item, system, materializer)).toSet
            // Returned bikes are those which were rented and now are available
            (available &~ bikes.bikes).foreach(db.insertBike)
            if(!allUpdated){
              println("Updating all!")
              val updatable = available.filter(bikes.bikes.contains).view.map(b => b.id -> b).toMap
              var counter = 0
              bikes.bikes.filter(available.contains).foreach(b => {
                if(b.latitude < 1){
                  println(s"${b.id} ${b.latitude} ${b.longitude}")
                  println(s"${updatable(b.id).id} ${updatable(b.id).latitude} ${updatable(b.id).longitude}")
                }
                b.setCoords(updatable(b.id))
                counter += 1
              })
              println(updatable.size)
              println(counter)
              allUpdated = true
            }

            bikes.returned = bikes.rented & available

            // Rented are those which are not available
            bikes.rented = bikes.bikes &~ available
            // The coordinates of rented and returned should be updated

            (bikes.rented | bikes.returned).foreach(_.updateCoords())

            // Bikes are all bikes that have been collected since the server started

            bikes.bikes = bikes.bikes | available
          case _ =>
            println("Problem")
        })
    case resp @ HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      resp.discardEntityBytes()
    case _ => log.info("Unsupported message")
  }
}

class Bikes(val system: ActorSystem,val materializer: ActorMaterializer, var bikes: Set[Bike] = Set(),
            var returned: Set[Bike] = Set(), var rented: Set[Bike] = Set()){

  def this(system: ActorSystem, materializer: ActorMaterializer, ids: Future[Seq[Int]]){
    this(system, materializer)
    import scala.concurrent.ExecutionContext.Implicits.global
    ids.onComplete({
      case Success(bikeIDs) =>
        bikes =  bikes ++ bikeIDs.map(id => new Bike(id, system, materializer))
        updateAll()
      case Failure(_) => println("Cannot get bikes from database")
    })
  }


  private val updator = system.actorOf(Props(classOf[BikesUpdator], this, system, materializer), "bikes")


  def update(): Unit = {
    updator ! Updating
  }

  def updateAll(): Unit = {
    updator ! UpdateAll
  }
}

object Bikes {
  case object Updating
  case object UpdateAll
  val url: String = "https://app.socialbicycles.com/api/bikes.json?network_id=105&per_page=10000"
}
