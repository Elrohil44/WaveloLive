package Bikes

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import spray.json._

import scala.collection.mutable
import scala.util.Success

/**
  * Created by Wiesiek on 2017-05-20.
  */

class Bikes extends JsonSupport {
  var bikes: mutable.Set[Bike] = mutable.Set()
  var returned: mutable.Set[Bike] = mutable.Set()
  var rented: mutable.Set[Bike] = mutable.Set()


  def Update(): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    val response = Http().singleRequest(HttpRequest(uri = Bikes.url))
      .onComplete({
        case Success(res) =>
          res match {
            case HttpResponse(StatusCodes.OK, _, entity, _) =>
              val body = Unmarshal(entity).to[String]
                .onComplete({
                  case Success(json) =>
                    val _list = json.parseJson.convertTo[BikeList].items
                    var coords: Array[Double] = null
                    var available: mutable.Set[Bike] = mutable.Set()
                    available ++= (for (item <- _list)  yield {
                      coords = item.current_position.coords
                      new Bike(item.id, coords(1), coords(0))
                    }).toSet
                    returned = rented intersect available
                    rented = bikes -- available
                    for (toupdate <- rented ++ returned)
                      toupdate.synchronized {
                        toupdate.updateCoords()
                      }
                    bikes ++= available
                  case _ =>
                    println("Problem")
                })

            case _ => println("Problem!")
          }
        case _ => println("Problem")
      })
  }

//  Update()

}

object Bikes {
  val url: String = "https://app.socialbicycles.com/api/bikes.json?network_id=105&per_page=10000"
}
