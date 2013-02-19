package Narcissism

import concurrent.{Await, ExecutionContext, Future}
import concurrent.duration._
import ExecutionContext.Implicits.global
import util.{Success, Failure}

/**
 * Daniel Schobel
 * Date: 19/02/13
 * Time: 2:38 PM
 */


object TwitterPoller {
  val secret_file = "./secrets.json"
  val tw = TwitterAPI(secret_file)

  private def read_data():Set[User] ={

    var res: Set[User] = null
    res = Set(User("desc","id_str","location","name","profile_image","screen_name",true))
    res
  }

  private def save_data(new_users: Set[User]): Unit={
    println("saving my data!")
  }


  private def poll_and_process(): Unit = {
    synchronized {
      //read followers list from persistence
      val previous_users = Future{ read_data }

      //read followers list from twitter
      val current_users = tw getFollowers "dschobel"
      current_users.onSuccess{case followers => println("I found: " + followers.size)}

      //set difference
      val missing: Future[Set[User]] = for (previous: Set[User] <- previous_users; current: Set[User] <- current_users)  yield(previous -- current)

      //record missing items into new table
      missing map{save_data(_)}

      missing onFailure { case e => println(e) }
      missing onSuccess { case res => println("yay, it worked!")}
    }
  }


  def main(args: Array[String]){
    while(true){
      poll_and_process()
      Thread.sleep(2 * 60 * 1000);
      //Await.ready(pp,  5 minutes)
    }
  }
}
