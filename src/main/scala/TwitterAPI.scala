import concurrent.{Await, Promise, ExecutionContext, Future}
import ExecutionContext.Implicits.global
import org.apache.http.util.EntityUtils
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import scala.util.parsing.json._
import scala.concurrent.duration._


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
  def getFollowers(screen_name: String): Future[Seq[User]] = {
    val request = s"http://api.twitter.com/1.1/followers/ids.json?cursor=-1&screen_name=$screen_name"
    val ids = getResponseBody(request).map{TwitterAPI.parseIds(_)}
    //for ( id <- ids; users <- getUsersFromIds(id)) yield users
    ???
  }

  private def parseUserResponse(response: String): Seq[User]={
    ???
  }

  private def makeUserRequest(ids: List[BigDecimal]): String = {
    ???
  }

  def gather_futures[A](xs: Seq[Future[A]]): Future[Seq[A]]= {
    def combine[A,B,C](f1: Future[A], f2: Future[B])(f: (A,B) => C): Future[C] ={
      for(a <- f1; b <- f2)
      yield f(a,b)
    }
    xs.foldLeft(Future{Seq[A]()}){(acc: Future[Seq[A]],x: Future[A]) => combine(x,acc)((a: A,b: Seq[A]) => b ++ Seq(a))}
  }


  def getUsersFromIds(ids: List[BigDecimal], batch_size:Int = 100): Seq[Future[Seq[User]]] = {

    val queryForUsers: (List[BigDecimal]) => Future[String] = (makeUserRequest _) andThen getResponseBody

    // for(group: List[BigDecimal] <- ids.sliding(100,100);
    //  response: String <- queryForUsers(group)
    //) yield parseUserResponse(response)

    val list_of_future_responses: Seq[Future[String]] =
      for(id_group <- (ids.sliding(batch_size,batch_size)) toSeq) yield queryForUsers(id_group)
    val future_users: Seq[Future[Seq[User]]] = list_of_future_responses.map {_.map{parseUserResponse(_)}}

    future_users
  }
}
