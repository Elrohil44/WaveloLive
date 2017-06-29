package Database

import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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
  val db = Database.forConfig("databaseUrl")
  db.createSession()

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
    db.run(dbbikes += bike.id)
  }

}
