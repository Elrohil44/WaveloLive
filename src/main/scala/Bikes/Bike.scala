package Bikes

import Bikes.Updating
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import spray.json._

import scala.concurrent.duration._
import scala.util.Success

/**
  * Created by Wiesiek on 2017-05-20.
  */

// Defining Case Classes for Spray JSON
// It allows it to parse JSON into object of that class
// or serialize object into JSON

case class PositionJSON(_type: String, coords: Array[Double])

case class BikeJSON(id: Int, name: String, network_id: Int,
                    hub_id: Option[Int], inside_area: Boolean, distance: Option[Double],
                    address: String, sponsored: Boolean, current_position: PositionJSON)

case class BikesJSON(bikes: Array[Bike])

case class BikeList(current_page: Int, per_page: Int, total_entries: Int,
                    items: Array[BikeJSON])

// Because I cannot create Case Class for my defined Bike Class
// I create object responsible for (re)serializing it

trait BikeJsonProtocol extends DefaultJsonProtocol {
  implicit object BikeJsonFormat extends RootJsonFormat[Bike] {
    def write(obj: Bike) =
      JsObject(("id", JsNumber(obj.id)), ("latitude", JsNumber(obj.latitude)),
        ("longitude", JsNumber(obj.longitude)))

    def read(value: JsValue) = value match {
      case JsObject(x) =>
        (x("id"), x("latitude"), x("longitude")) match{
        case (JsNumber(id), JsNumber(latitude), JsNumber(longitude)) =>
            new Bike(id.toInt, null, null, latitude.toDouble, longitude.toDouble)
        case _ => deserializationError("Bike expected")
    }
      case _ => deserializationError("Bike expected")
    }
  }
}

// Creating trait, where it is said what is the format of each Case Class defined above
// In this way, Spray JSON knows how to (re)serialize them

trait JsonSupport extends SprayJsonSupport with BikeJsonProtocol {
  implicit val positionFormat = jsonFormat(PositionJSON, "type", "coordinates")
  implicit val bikeJSONFormat = jsonFormat9(BikeJSON)
  implicit val bikesFormat = jsonFormat1(BikesJSON)
  implicit val bikeListFormat = jsonFormat4(BikeList)
}


class BikeUpdator(val biike: Bike, implicit val system: ActorSystem,
                  implicit val materializer: ActorMaterializer) extends Actor with JsonSupport{
  import akka.pattern.pipe
  import context.dispatcher

  val http = Http(system)

  def update(): Unit = {
    http.singleRequest(HttpRequest(uri = Bike.url + biike.id.toString)).pipeTo(self)
  }

  def receive = {
    case Updating => update()
    case HttpResponse(StatusCodes.OK, _, entity, _) =>
      val body = Unmarshal(entity).to[String]
        .onComplete({
          case Success(json) =>
            val bike = json.parseJson.convertTo[BikeJSON]
            biike.latitude = bike.current_position.coords(1)
            biike.longitude = bike.current_position.coords(0)
            biike.updated = true
          case _ =>
            println("Problem")
        })
    case resp @ HttpResponse(code, _, _, _) =>
      println("Error")
      resp.discardEntityBytes()
    case _ => system.scheduler.scheduleOnce(1.seconds, self, Updating)
  }

}

class Bike(val id: Int, implicit val system: ActorSystem,
           implicit val materializer: ActorMaterializer,
           var latitude: Double = 0.0, var longitude: Double = 0.0){

  def this(bike: BikeJSON, system: ActorSystem, materializer: ActorMaterializer){
    this(bike.id, system, materializer, bike.current_position.coords(1), bike.current_position.coords(0))
  }

  var updated: Boolean = false
  private var withActor: Boolean = false
  private var updator: ActorRef = _


  def updateCoords(): Unit = {
    if(!withActor){
      updator = system.actorOf(Props(classOf[BikeUpdator], this, system, materializer), "Bike" + id.toString)
      withActor = true
    }
    updator ! Updating
  }

  // This two methods aren't really necessary, I have created them at the beginning of the project

  def setCoords(latitude: Double, longitude: Double): Unit = {
    this.latitude = latitude
    this.longitude = longitude
  }

  def printCoords(): Unit = {
    println("Bike" +" "+id+" " + latitude + " " + longitude)
  }


  // These methods are necessary for Set(), to determine if it is the same bike

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