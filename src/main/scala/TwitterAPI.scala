import concurrent.{Await, ExecutionContext, Future}
import ExecutionContext.Implicits.global
import org.apache.http.util.EntityUtils
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import scala.util.parsing.json._
import scala.concurrent.duration._


object TwitterAPI {
  //JSON.globalNumberParser = {input : String => BigDecimal(input)}
  JSON.globalNumberParser = {input : String => input}
  def apply(filename: String)= new TwitterAPI(readSecrets(filename))

  def readSecrets(filename: String)={
    val source = scala.io.Source.fromFile(filename)
    val lines = source .mkString
    source.close ()

    JSON.parseFull(lines).asInstanceOf[Option[Map[String,String]]] getOrElse Map.empty
  }

  def parseUsers(user_data: List[Map[String,Any]]): Seq[User]={
     for {
                  fields <- user_data;
                  JSONExtractor.S(id) = fields("id_str")
                  JSONExtractor.S(name) = fields("name")
                  JSONExtractor.S(location) = fields("location")
                  JSONExtractor.S(profile_image_url) = fields("profile_image_url")
                  JSONExtractor.S(screen_name) = fields("screen_name")
                  JSONExtractor.S(description) = fields("description")
                  JSONExtractor.B(following) = fields("following")
            } yield User(description,id, location,name,profile_image_url,screen_name,following)
  }

  def parseUserResponse(json_string: String): Seq[User]={


    ???
  }

  private def parseIds(json: String): Seq[String] ={
    (for{
      JSONExtractor.M(fields: Map[String,Any]) <- JSON.parseFull(json).toIterable;
      JSONExtractor.L(list_of_ids) = fields("ids")
      id <- list_of_ids.map {_.toString}
    } yield id) toSeq
  }
  /* (for (map <- JSON.parseFull(json).asInstanceOf[Option[Map[String,Any]]];
              list <- map.get("ids").asInstanceOf[Option[List[BigDecimal]]]
         ) yield list).getOrElse(Nil)*/

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
  def getFollowers(screen_name: String): Future[Seq[User]] = {
    val request = s"http://api.twitter.com/1.1/followers/ids.json?cursor=-1&stringify_ids=true&screen_name=$screen_name"
    val ids = getResponseBody(request).map{TwitterAPI.parseIds(_)}
    for ( id_futures <- ids; users <- getUsersFromIds(id_futures)) yield users
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


    val response = Await.result(queryForUsers(ids), 30 seconds)
    // for(group: List[BigDecimal] <- ids.sliding(100,100);
    //  response: String <- queryForUsers(group)
    //) yield parseUserResponse(response)

    //[{"id":14959032,"entities":{"url":{"urls":[{"expanded_url":null,"url":"http:\/\/www.2degreesrecruitment.com.au","indices":[0,37],"display_url":null}]},"description":{"urls":[]}},"followers_count":280,"url":"http:\/\/www.2degreesrecruitment.com.au","contributors_enabled":false,"time_zone":"Sydney","profile_background_color":"C0DEED","utc_offset":36000,"verified":false,"default_profile":true,"name":"Diane Cotterill","geo_enabled":false,"lang":"en","profile_background_image_url":"http:\/\/a0.twimg.com\/images\/themes\/theme1\/bg.png","location":"Sydney, Australia","profile_link_color":"0084B4","protected":false,"profile_image_url":"http:\/\/a0.twimg.com\/profile_images\/2537252057\/np4polio0c7woe2m9v2k_normal.jpeg","listed_count":13,"profile_use_background_image":true,"notifications":false,"follow_request_sent":false,"screen_name":"dmcotter","profile_text_color":"333333","profile_image_url_https":"https:\/\/twimg0-a.akamaihd.net\/profile_images\/2537252057\/np4polio0c7woe2m9v2k_normal.jpeg","id_str":"14959032","following":false,"profile_background_image_url_https":"https:\/\/twimg0-a.akamaihd.net\/images\/themes\/theme1\/bg.png","is_translator":false,"profile_sidebar_border_color":"C0DEED","default_profile_image":false,"status":{"entities":{"hashtags":[],"user_mentions":[],"urls":[]},"place":null,"coordinates":null,"retweeted":false,"id_str":"302293857301848064","contributors":null,"in_reply_to_user_id":null,"in_reply_to_status_id":null,"retweet_count":1,"in_reply_to_status_id_str":null,"favorited":false,"in_reply_to_screen_name":null,"text":"They also want front-end developers, iOS developers and Sys Engineers so ping me for more info people of the globe :)\n   2\/2","in_reply_to_user_id_str":null,"geo":null,"truncated":false,"source":"web","id":302293857301848064,"created_at":"Fri Feb 15 05:50:46 +0000 2013"},"created_at":"Fri May 30 23:58:42 +0000 2008","favourites_count":660,"friends_count":540,"profile_background_tile":false,"description":"Founder of 2 degrees recruitment, a referral based recruitment company looking to connect great people in the IT industry. ","profile_sidebar_fill_color":"DDEEF6","statuses_count":690}]

    val list_of_future_responses: Seq[Future[String]] =
      for(id_group <- (ids.sliding(batch_size,batch_size)) toSeq) yield queryForUsers(id_group)
    val future_users: Seq[Future[Seq[User]]] = list_of_future_responses.map {_.map{TwitterAPI.parseUserResponse(_)}}

    gather_and_flatten(future_users)
  }
}
