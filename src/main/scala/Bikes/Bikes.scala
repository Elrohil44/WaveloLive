package Bikes

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import spray.json._
import scala.util.Success
import Bikes.Updating

/**
  * Created by Wiesiek on 2017-05-20.
  */

class BikesUpdator(val bikes: Bikes, implicit val system: ActorSystem, implicit val materializer: ActorMaterializer)
  extends Actor with ActorLogging with JsonSupport{
  import akka.pattern.pipe
  import context.dispatcher

  val http = Http(system)

  def update(): Unit = {
    http.singleRequest(HttpRequest(uri = Bikes.url)).pipeTo(self)
  }

  def receive = {
    case Updating => update()
    case HttpResponse(StatusCodes.OK, _, entity, _) =>
      val body = Unmarshal(entity).to[String]
        .onComplete({
          case Success(json) =>
            val _list = json.parseJson.convertTo[BikeList].items

            // I don't know if it is very effective but it works
            // It adds bikes that were rented and haven't been collected by the server since
            // the server started

            val available = (for (item <- _list) yield new Bike(item, system, materializer)).toSet
            // Returned bikes are those which were rented and now are available

            bikes.returned = bikes.rented & available

            // Rented are those which are not available

            bikes.rented = bikes.bikes -- available
            // The coordinates of rented and returned should be updated

            for (toupdate <- bikes.rented ++ bikes.returned)
              toupdate.updateCoords()

            // Bikes are all bikes that have been collected since the server started

            bikes.bikes = bikes.bikes ++ available
          case _ =>
            println("Problem")
        })
    case resp @ HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      resp.discardEntityBytes()
    case _ => log.info("Unsupported message")
  }
}

class Bikes(implicit val system: ActorSystem,implicit val materializer: ActorMaterializer){
  var bikes: Set[Bike] = Set()
  var returned: Set[Bike] = Set()
  var rented: Set[Bike] = Set()
  private val updator = system.actorOf(Props(classOf[BikesUpdator], this, system, materializer), "bikes")

  def update(): Unit = {
    updator ! Updating
  }
}

object Bikes {
  case object Updating
  val url: String = "https://app.socialbicycles.com/api/bikes.json?network_id=105&per_page=10000"
}
