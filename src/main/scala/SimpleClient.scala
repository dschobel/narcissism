import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Success,Failure}
import ExecutionContext.Implicits.global

class SimpleClient extends App {
  val secret_file = "./secrets.json"

  val tw = TwitterAPI(secret_file)
  val futureFollowers = tw.getFollowers("dschobel")


  futureFollowers.onComplete{
    case Success(followers) =>  println((followers.map {_.id_str}).mkString(", "))
    case Failure(err) => println("something went wrong\n" + err.toString)
  }

  println("waiting for result")
  Await.result(futureFollowers,2 minutes)

}
