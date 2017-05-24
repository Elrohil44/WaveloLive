package Bikes

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import spray.json._

import scala.util.Success

/**
  * Created by Wiesiek on 2017-05-20.
  */


case class PositionJSON(_type: String, coords: Array[Double])
final case class BikeJSON(id: Int, name: String, network_id: Int,
                    hub_id: Option[Int], inside_area: Boolean, distance: Option[Double],
                    address: String, sponsored: Boolean, current_position: PositionJSON)
case class BikesJSON(bikes: Array[Bike])
//case class GETBike(id: Int, latitude: Double, longitude: Double)
case class BikeList(current_page: Int, per_page: Int, total_entries: Int,
                    items: Array[BikeJSON])

trait BikeJsonProtocol extends DefaultJsonProtocol {
  implicit object BikeJsonFormat extends RootJsonFormat[Bike] {
    def write(obj: Bike) =
      JsObject(("id", JsNumber(obj.id)), ("latitude", JsNumber(obj.latitude)),
        ("longitude", JsNumber(obj.longitude)))

    def read(value: JsValue) = value match {
      case JsObject(x) =>
        (x("id"), x("latitude"), x("longitude")) match{
        case (JsNumber(id), JsNumber(latitude), JsNumber(longitude)) =>
            new Bike(id.toInt, latitude.toDouble, longitude.toDouble)
        case _ => deserializationError("Bike expected")
    }
      case _ => deserializationError("Bike expected")
    }
  }
}



trait JsonSupport extends SprayJsonSupport with BikeJsonProtocol {
  implicit val positionFormat = jsonFormat(PositionJSON, "type", "coordinates")
  implicit val bikeJSONFormat = jsonFormat9(BikeJSON)
//  implicit val bikeFormat = jsonFormat3(GETBike)
  implicit val bikesFormat = jsonFormat1(BikesJSON)
  implicit val bikeListFormat = jsonFormat4(BikeList)
}


class Bike(val id: Int, var latitude: Double = 0.0,
           var longitude: Double = 0.0) extends Directives with JsonSupport{

  def updateCoords(): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val response = Http().singleRequest(HttpRequest(uri = Bike.url + id.toString))
      .onComplete({
        case Success(res) =>
          res match {
            case HttpResponse(StatusCodes.OK, _, entity, _) =>
              val body = Unmarshal(entity).to[String]
                .onComplete({
                  case Success(json) =>
                    val bike = json.parseJson.convertTo[BikeJSON]
                    latitude = bike.current_position.coords(1)
                    longitude = bike.current_position.coords(0)
                  case _ =>
                    println("Problem")
                })
            case _ => println("Problem!")
          }
        case _ => println("Problem")
      })

  }

  def setCoords(latitude: Double, longitude: Double): Unit = {
    this.latitude = latitude
    this.longitude = longitude
  }

  def printCoords(): Unit = {
    println("Bike" +" "+id+" " + latitude + " " + longitude)
  }


  override def equals(other: Any): Boolean = other match {
    case that: Bike => (that canEqual this) && (id == that.id)
    case _ => false
  }

  override def hashCode(): Int = 0

  def canEqual(other: Any): Boolean = other.isInstanceOf[Bike]

}


object Bike{
  val url: String = "https://app.socialbicycles.com/api/bikes/"
}