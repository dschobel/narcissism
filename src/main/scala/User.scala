import concurrent.{ExecutionContext, Future, Promise}
import ExecutionContext.Implicits.global

/**
 * User: daniel
 * Date: 11/02/13
 * Time: 4:43 PM
 */

case class User(description: String, id_str: String,location: String,name: String, profile_image_url: String, screen_name: String)

object UserRequester{


}

class UserRequester(username: String, accessToken: String, accessKey: String, consumerKey: String, consumerSecret: String) {
  private val _username = username
  private val _accessToken = accessToken
  private val _consumerKey = consumerKey
  private val _consumerSecret = consumerSecret
}
