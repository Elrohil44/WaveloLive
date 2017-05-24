import Bikes.{Bikes, BikesJSON, JsonSupport}
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn

object WebServer extends JsonSupport {

  case object GetBikes
  case object GetToUpdate
  case object Update

  var bikes = new Bikes()

  class Updater extends Actor with ActorLogging {
    def receive = {
      case Update => bikes.Update()
      case _ => log.info("Invalid message")
    }
  }

  class Retriever extends
    Actor with ActorLogging {
    def receive = {
      case GetToUpdate => sender() ! BikesJSON((bikes.rented ++ bikes.returned).toArray)
      case GetBikes => sender() ! BikesJSON(bikes.bikes.toArray)
      case _ => log.info("Invalid message")
    }
  }
  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val updater = system.actorOf(Props[Updater], "updater")
    val retriever = system.actorOf(Props[Retriever], "retriever")
    system.scheduler.schedule(0.seconds, 30.seconds, updater, Update)

    val route =
      path("bikes") {
          get {
            implicit val timeout: Timeout = 10.seconds

            // query the actor for the current auction state
            val bikes: Future[BikesJSON] = (retriever ? GetBikes).mapTo[BikesJSON]
            complete(bikes)
          }
      } ~
        path("toupdate") {
          get {
            implicit val timeout: Timeout = 10.seconds

            // query the actor for the current auction state
            val bikes: Future[BikesJSON] = (retriever ? GetToUpdate).mapTo[BikesJSON]
            complete(bikes)
          }
        }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8089)
    println(s"Server online at http://localhost:8089/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done

  }
}