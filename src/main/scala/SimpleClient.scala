import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Success,Failure}
import ExecutionContext.Implicits.global

object SimpleClient extends App {
  val secret_file = "./secrets.json"

  val tw = TwitterAPI(secret_file)
 /* val futureFollowers = tw.getFollowers("dschobel")


  futureFollowers.onComplete{
    case Success(followers) =>  println((followers.map {_.id_str}).mkString(", "))
    case Failure(err) => println("something went wrong\n" + err.toString)
  }*/

  val future_user_details = tw.getUsersFromIds(List("14959032"))
  println("waiting for result")
  Await.result(future_user_details,2 minutes)

}
