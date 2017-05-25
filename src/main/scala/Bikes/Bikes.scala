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

    // I didn't find the request to get all the bikes
    // I have found only how to get those which are available (means not rented at the moment)

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

                    // I don't know if it is very effective but it works
                    // It adds bikes that were rented and haven't been collected by the server since
                    // the server started

                    available ++= (for (item <- _list)  yield {
                      new Bike(item)
                    }).toSet

                    // Returned bikes are those which were rented and now are available

                    returned = rented intersect available

                    // Rented are those which are not available

                    rented = bikes -- available

                    // The coordinates of rented and returned should be updated

                    for (toupdate <- rented ++ returned)
                      toupdate.updateCoords()

                    // Bikes are all bikes that have been collected since the server started

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
