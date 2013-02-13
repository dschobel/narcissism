import concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import org.apache.http.util.EntityUtils
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import scala.util.parsing.json._


object TwitterAPI {

  def apply(filename: String)= new TwitterAPI(readSecrets(filename))

  def readSecrets(filename: String)={
    val source = scala.io.Source.fromFile(filename)
    val lines = source .mkString
    source.close ()

    JSON.parseFull(lines).asInstanceOf[Option[Map[String,String]]] getOrElse Map.empty
  }

  private def parseIds(json: String): List[BigDecimal] =
        (for (map <- JSON.parseFull(json).asInstanceOf[Option[Map[String,Any]]];
              list <- map.get("ids").asInstanceOf[Option[List[BigDecimal]]]
         ) yield list).getOrElse(Nil)

}

class TwitterAPI(conf: Map[String,String]){
  private val config = conf

  JSON.globalNumberParser = {input : String => BigDecimal(input)}
  val consumer = new CommonsHttpOAuthConsumer(config("ConsumerKey"),config("ConsumerSecret"))
  consumer.setTokenWithSecret(config("AccessToken"), config("AccessSecret"))

  private def getBlockingResponseBody(requestString: String)= {
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
  private def getResponseBody(request: String) = Future{getBlockingResponseBody(request) }

  /**
   * Given a twitter screen name, returns the followers, capped at 5000
   *
   * @param screen_name
   * @return
   */
  def getFollowers(screen_name: String): Future[List[User]] = {
    val request = s"http://api.twitter.com/1.1/followers/ids.json?cursor=-1&screen_name=$screen_name"
    val ids = getResponseBody(request).map{TwitterAPI.parseIds(_)}
    for ( id <- ids; users <- getUsersFromIds(id)) yield users
  }

  def getUsersFromIds(ids: List[BigDecimal]): Future[List[User]] = {
    Future{ids.map{id => User("desc",id.toString,"loc","name","profile_url","screenname")}}
  }
}
