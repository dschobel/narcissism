import scala.concurrent.duration._
import concurrent.{Await, ExecutionContext, Future}
import ExecutionContext.Implicits.global
import org.apache.http.util.EntityUtils
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import scala.util.parsing.json._
import util.{Success, Failure}


object TwitterAPI {

  val secret_file = "./secrets.json"
  lazy val secrets: Map[String,String] = readSecrets(secret_file)


  val consumer = new CommonsHttpOAuthConsumer(secrets("ConsumerKey"),secrets("ConsumerSecret"))
  consumer.setTokenWithSecret(secrets("AccessToken"), secrets("AccessSecret"))

  JSON.globalNumberParser = {input : String => BigDecimal(input)}

  def readSecrets(filename: String)={
    val source = scala.io.Source.fromFile(filename)
    val lines = source .mkString
    source.close ()

    JSON.parseFull(lines).asInstanceOf[Option[Map[String,String]]] getOrElse Map.empty
  }

  def getResponseBody(requestString: String): String= {
    val request = new HttpGet(requestString)
    consumer.sign(request)
    val client = new DefaultHttpClient()
    val response = client.execute(request)
    val entity = response.getEntity()
    var respMessage = new String()
    if(entity != null){
      respMessage = EntityUtils.toString(entity)
    }
    respMessage
  }
  def getFutureResponseBody(request: String): Future[String]= Future{getResponseBody(request) }

  /**
   * Given a twitter screen name, returns the followers, capped at 5000
   *
   * @param screen_name
   * @return
   */
  def getFollowers(screen_name: String):Future[List[User]] = {
    val processJSON = parseIds _ andThen getUsersFromIds
    val request = s"http://api.twitter.com/1.1/followers/ids.json?cursor=-1&screen_name=$screen_name"
    getFutureResponseBody(request).map {resp => processJSON(resp)}
  }

  def parseIds(json: String): List[BigDecimal]={
    val res = (for (map <- JSON.parseFull(json).asInstanceOf[Option[Map[String,Any]]];
         list <- map.get("ids").asInstanceOf[Option[List[BigDecimal]]]
    ) yield list).getOrElse(Nil)
    res
  }

  def getUsersFromIds(ids: List[BigDecimal]): List[User] = {
    ids.map{id => User("desc",id.toString,"loc","name","profile_url","screenname")}
  }

  def main(args: Array[String]) {
    val futureFollowers = TwitterAPI.getFollowers("dschobel")

    futureFollowers.onComplete{
      case Success(followers) =>  println((followers.map {_.id_str}).mkString(", "))
      case Failure(err) => println("something went wrong\n" + err.toString)
    }

    println("waiting for result")
    Await.result(futureFollowers,2 minutes)
  }
}
