package Server

import Bikes.{Bikes, BikesJSON, JsonSupport}
import Database.BikeDatabase
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by Wiesiek on 2017-06-28.
  */
object WebServer extends JsonSupport with App{

  // Case objects defined to identify requests for actors

  case object GetBikes
  case object GetToUpdate
  case object GetRented
  case object GetAvailable
  case object Update

  // bikes stores information about all bikes collected by server

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()

  val config = ConfigFactory.load()
  val logger = Logging(system, getClass)
  private val db = BikeDatabase

  private val bikes = new Bikes(system, materializer, db.getBikes)
  // Actor Updater is responsible for updating information about bikes

  class Updater extends Actor with ActorLogging {
    def receive = {
      case Update => bikes.update()
      case _ => log.info("Invalid message")
    }
  }

  // Actor Retriever is responsible for passing and retrieving information about bikes

  class Retriever extends
    Actor with ActorLogging {
    def receive = {
      case GetToUpdate => sender() ! BikesJSON((bikes.rented | bikes.returned).toArray)
      case GetBikes => sender() ! BikesJSON(bikes.bikes.toArray)
      case GetAvailable => sender() ! BikesJSON((bikes.bikes &~ bikes.rented).toArray)
      case GetRented => sender() ! BikesJSON(bikes.rented.toArray)
      case _ => log.info("Invalid message")
      }
  }

  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  // Initialising actors
  val updater = system.actorOf(Props[Updater], "updater")
  val retriever = system.actorOf(Props[Retriever], "retriever")

  // Schedule update interval to 30 seconds
  // Scheduler sends Update request to actor updater every 30 seconds
  system.scheduler.schedule(15.minutes, 30.minutes, () => {bikes.updateAll()})
  system.scheduler.schedule(10.seconds, 5.seconds, updater, Update)

  // Defining timeout (following the example xD)

  implicit val timeout: Timeout = 10.seconds
  // Defining routes



  val route = respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
    path("bikes") {
      get {
        val bikesJSON: Future[BikesJSON] = (retriever ? GetBikes).mapTo[BikesJSON]

        // After get request, complete returns JSON formatted bikes
        complete(bikesJSON)
      }
    } ~
    path("toupdate") {
      get {
        val bikesJSON: Future[BikesJSON] = (retriever ? GetToUpdate).mapTo[BikesJSON]

        // As said before
        complete(bikesJSON)
      }
    } ~
    path("rented") {
      get {
        val bikesJSON: Future[BikesJSON] = (retriever ? GetRented).mapTo[BikesJSON]

        // As said before
        complete(bikesJSON)
      }
    } ~
    path("available") {
      get {
        val bikesJSON: Future[BikesJSON] = (retriever ? GetAvailable).mapTo[BikesJSON]

        // As said before
        complete(bikesJSON)
      }
    }
  }


    // Setting server address and port

  val bindingFuture = Http().bindAndHandle(route, config.getString("http.interface"), config.getInt("http.port"))
}
