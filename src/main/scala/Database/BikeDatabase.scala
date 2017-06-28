package Database

import com.typesafe.config.ConfigFactory
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by Wiesiek on 2017-06-28.
  */
object BikeDatabase {
  class Bike(tag: Tag) extends Table[Int](tag, "BIKES"){
    def id = column[Int]("ID", O.PrimaryKey)
    def * = id
  }

  val dbbikes = TableQuery[Bike]
  val config = ConfigFactory.load()
  val db = Database.forConfig("databaseUrl")

  db.run(MTable.getTables("BIKES")).onComplete({
    case Success(res) =>
      if(res.toList.isEmpty){
        db.run(dbbikes.schema.create)
      }
    case Failure(_) => println("Failure")
  })

  def getBikes: Future[Seq[Int]] = {
    val query = (for (bike <- dbbikes) yield bike.id).result
    db.run(query)
  }

  def insertBike(bike: Bikes.Bike) = {
    val query = db.run(dbbikes += bike.id)
    val queryy = (for (bike <- dbbikes) yield bike.id).result
    val q = db.run(queryy)
    var i = 0
    Await.result(q, 5.seconds).foreach((_:Int) => i+=1)
    println(i)
  }

}
