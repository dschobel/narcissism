package narcissism

import concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import org.apache.http.util.EntityUtils
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import scala.util.parsing.json._


case class User(description: String, id_str: String,location: String,name: String, profile_image_url: String, screen_name: String, following: Boolean)

object TwitterAPI {
  //JSON.globalNumberParser = {input : String => BigDecimal(input)}
  JSON.globalNumberParser = {input : String => input}


  def parseUserResponse(json_string: String): Seq[User]={
      (for{
        JSONExtractor.L(stuff) <- JSON.parseFull(json_string).toIterable
        JSONExtractor.M(fields) <- stuff
        JSONExtractor.S(id) = fields("id_str")
        JSONExtractor.S(name) = fields("name")
        JSONExtractor.S(location) = fields("location")
        JSONExtractor.S(profile_image_url) = fields("profile_image_url")
        JSONExtractor.S(screen_name) = fields("screen_name")
        JSONExtractor.S(description) = fields("description")
        JSONExtractor.B(following) = fields("following")
        } yield User(description,id, location,name,profile_image_url,screen_name,following)) toSeq
  }

  private def parseIds(json: String): Seq[String] ={
    (for{
      JSONExtractor.M(fields) <- JSON.parseFull(json).toIterable;
      JSONExtractor.L(list_of_ids) = fields("ids")
      id <- list_of_ids.map {_.toString}
    } yield id) toSeq
  }
}

class TwitterAPI(conf: Map[String,String]){
  private val config = conf


  val consumer = new CommonsHttpOAuthConsumer(config("ConsumerKey"),config("ConsumerSecret"))
  consumer.setTokenWithSecret(config("AccessToken"), config("AccessSecret"))

  private def makeBlockingRequest(requestString: String)= {
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
  private def getResponseBody(request: String) = Future{makeBlockingRequest(request) }

  /**
   * Given a twitter screen name, returns the followers, capped at 5000
   *
   * @param screen_name
   * @return
   */
  def getFollowers(screen_name: String): Future[Set[User]] = {
    val request = s"http://api.twitter.com/1.1/followers/ids.json?cursor=-1&stringify_ids=true&screen_name=$screen_name"
    val ids = getResponseBody(request).map{TwitterAPI.parseIds(_)}
    for ( id_futures <- ids; users <- getUsersFromIds(id_futures)) yield users.toSet
  }




  private def gather_and_flatten[A](xs: Seq[Future[Seq[A]]]): Future[Seq[A]]= {
    def combine[A,B,C](f1: Future[A], f2: Future[B])(f: (A,B) => C): Future[C] ={
      for(a <- f1; b <- f2)
      yield f(a,b)
    }
    val empty_future: Future[Seq[A]] = Future{Nil}

    val fx = (acc: Future[Seq[A]],x: Future[Seq[A]]) => combine(x,acc)((a,b) => b ++ Seq(a).flatten)
    xs.foldLeft(empty_future){fx}
  }

  private def gather_futures[A](xs: Seq[Future[A]]): Future[Seq[A]] = {
    def combine[A,B,C](f1: Future[A], f2: Future[B])(f: (A,B) => C): Future[C] ={
      for(a <- f1; b <- f2) yield f(a,b)
    }
    val empty = Future{Seq[A]()}
    xs.foldLeft(empty){(acc: Future[Seq[A]],x: Future[A]) => combine(x,acc)((a: A,b: Seq[A]) => b ++ Seq(a))}
  }


  def getUsersFromIds(ids: Seq[String], batch_size:Int = 100): Future[Seq[User]] = {
    if (batch_size > 100) throw new Error("batch_size cannot exceed 100")

    def makeUserRequest(ids: Seq[String]): String = {
      val comma_separated_ids = ids.mkString(",")
      s"https://api.twitter.com/1.1/users/lookup.json?user_id=$comma_separated_ids"
    }

    val queryForUsers = (makeUserRequest _) andThen getResponseBody

    val list_of_future_responses: Seq[Future[String]] =
      for(id_group <- (ids.sliding(batch_size,batch_size)) toSeq) yield queryForUsers(id_group)
    val future_users: Seq[Future[Seq[User]]] = list_of_future_responses.map {_.map{TwitterAPI.parseUserResponse(_)}}

    gather_and_flatten(future_users)
  }
}
