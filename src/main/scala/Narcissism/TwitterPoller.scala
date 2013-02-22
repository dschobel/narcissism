package narcissism

import concurrent.{Await, ExecutionContext, Future}
import ExecutionContext.Implicits.global
import util.{Success, Failure}
import scala.util.parsing.json.JSON
import org.squeryl.{Session, SessionFactory, Schema}
import org.squeryl.adapters.PostgreSqlAdapter
import org.squeryl.PrimitiveTypeMode._
import java.net.URI
import java.util.Properties


//import com.twitter.ostrich.stats.Stats

/**
 * Daniel Schobel
 * Date: 19/02/13
 * Time: 2:38 PM
 */

object Database{
  lazy val conn_string = Util.readSecrets("./secrets.json")("DBConnString")

  def init_session(): Unit = {
    val dbUri = new URI(conn_string)
    val username = dbUri.getUserInfo().split(":")(0)
    val password = dbUri.getUserInfo().split(":")(1)
    val dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath()
    val props = new Properties()
    props.setProperty("user",username)
    props.setProperty("password",password)
    props.setProperty("ssl","true")
    props.setProperty("sslfactory","org.postgresql.ssl.NonValidatingFactory")

    Class.forName("org.postgresql.Driver")
    SessionFactory.concreteFactory = Some(()=>
      Session.create(
        java.sql.DriverManager.getConnection(dbUrl,props),
        new PostgreSqlAdapter))
  }
}

object Util{
  def readSecrets(filename: String)= {
    val source = scala.io.Source.fromFile(filename)
    val lines = source.mkString
    source.close()
    JSON.parseFull(lines).asInstanceOf[Option[Map[String,String]]] getOrElse Map.empty
  }
}


object TwitterPoller {
  val secret_file = "./secrets.json"
  lazy val tw = new TwitterAPI(Util.readSecrets(secret_file))

  private def read_data():Set[User] ={
    var res: Set[User] = null
    //Stats.incr("reads")
    //Stats.time("read_data_timing") {
      res = Set(User("desc","id_str","location","name","profile_image","screen_name",true))
    //}
    res
  }

  private def save_data(new_users: Set[User]): Unit={
    //Stats.incr("writes")
    //Stats.time("save_data_timing") {
      println("saving my data!")
    //}
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

    Database.init_session()
    transaction {
      Unfollows.printDdl
      //books.insert(new Author(1, "Michel","Folco"))
      //val a = from(authors)(a=> where(a.lastName === "Folco") select(a))
    }


    while(true){
      poll_and_process()
      Thread.sleep(2 * 60 * 1000);
    }
  }
}
