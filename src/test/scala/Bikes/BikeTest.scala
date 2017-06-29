package Bikes

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.{TestActorRef, TestKit}
import org.scalatest.{FlatSpecLike, FunSuite, Matchers}

/**
  * Created by Wiesiek on 2017-06-30.
  */
class BikeTest extends TestKit(ActorSystem("Testsystem")) with FlatSpecLike with Matchers{
  implicit val materializer = ActorMaterializer()
  val json1 = """{
                  "id": 1,
                  "name": "1205",
                  "network_id": 105,
                  "hub_id": null,
                  "inside_area": true,
                  "distance": null,
                  "address": "Jana Długosza 3, Kraków",
                  "sponsored": false,
                  "current_position": {
                    "type": "Point",
                    "coordinates": [
                    1.0,
                    1.3
                    ]
                  }
                }"""
  val json2 = """{
                  "id": 5,
                  "name": "1205",
                  "network_id": 105,
                  "hub_id": null,
                  "inside_area": true,
                  "distance": null,
                  "address": "Jana Długosza 3, Kraków",
                  "sponsored": false,
                  "current_position": {
                    "type": "Point",
                    "coordinates": [
                    0.5,
                    0.1
                    ]
                  }
                }"""

  val bike = new Bike(1, system, materializer)
  val bike2 = new Bike(5, system, materializer)

  it should "Change coords" in {
    bike.setCoords(3.6, 2.4)
    bike.latitude shouldBe (3.6 +- 0.0001)
    bike.longitude shouldBe (2.4 +- 0.0001)
  }

  it should "Update coords after parsing json" in {
    val actorRef = TestActorRef(new BikeUpdator(bike, system, materializer))
    val actor = actorRef.underlyingActor

    val actorRef2 = TestActorRef(new BikeUpdator(bike2, system, materializer))
    val actor2 = actorRef2.underlyingActor
    actor.doUpdate(json1)
    actor2.doUpdate(json2)
    bike.latitude shouldBe (1.3 +- 0.0001)
    bike.longitude shouldBe (1.0 +- 0.0001)
    bike2.latitude shouldBe (0.1 +- 0.0001)
    bike2.longitude shouldBe (0.5 +- 0.0001)
  }

  it should "Set coords as in other bike" in {
    bike.setCoords(bike2)
    bike.latitude shouldBe (0.1 +- 0.0001)
    bike.longitude shouldBe (0.5 +- 0.0001)

  }

}
