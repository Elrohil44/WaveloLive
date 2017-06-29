package Bikes

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.{TestActorRef, TestKit}
import org.scalatest._

/**
  * Created by Wiesiek on 2017-06-29.
  */
class BikesTest extends TestKit(ActorSystem("Testsystem")) with FlatSpecLike with Matchers{
  implicit val materializer = ActorMaterializer()
  val json = """{
                 "current_page": 1,
                 "per_page": 10000,
                 "total_entries": 2,
                 "items": [
                   {
                     "id": 0,
                     "name": "0162",
                     "network_id": 105,
                     "hub_id": 2374,
                     "inside_area": true,
                     "distance": null,
                     "address": "Reymonta 27, Kraków",
                     "sponsored": false,
                     "current_position": {
                       "type": "Point",
                       "coordinates": [
                         1.0,
                         1.0
                       ]
                     }
                   },
                   {
                     "id": 1,
                     "name": "0225",
                     "network_id": 105,
                     "hub_id": 2367,
                     "inside_area": true,
                     "distance": null,
                     "address": "Grunwaldzka 8, Kraków",
                     "sponsored": false,
                     "current_position": {
                       "type": "Point",
                       "coordinates": [
                         0.0,
                         0.0
                       ]
                     }
                   }
                 ]
               }"""

  val bike0 = new Bike(0, system, materializer)
  val bike1 = new Bike(1, system, materializer)
  val bikes = new Bikes(system, materializer, withDB = false)

  it should "Update bike list" in {
    val actorRef = TestActorRef(new BikesUpdator(bikes, system, materializer))
    val actor = actorRef.underlyingActor
    actor.doUpdate(json)
    bikes.bikes.size shouldEqual  2
    bikes.bikes.contains(bike0) shouldBe true
    bikes.bikes.contains(bike1) shouldBe true
  }

  it should "Return bikes" in {
    bikes.bikes.contains(bike0) shouldBe true
    bikes.bikes.contains(bike1) shouldBe true
    bikes.bikes.size shouldEqual 2
  }
}
