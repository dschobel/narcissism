import org.apache.http.client.HttpClient
import org.apache.http.util.EntityUtils
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
 
 
object TwitterPull {
 
	  val AccessToken = "";
	  val AccessSecret = "";
	  val ConsumerKey = "";
	  val ConsumerSecret = "";
 
 
  def main(args: Array[String]) {
 
 	 val consumer = new CommonsHttpOAuthConsumer(ConsumerKey,ConsumerSecret)
	 consumer.setTokenWithSecret(AccessToken, AccessSecret)
 
     val request = new HttpGet("http://api.twitter.com/1.1/followers/ids.json?cursor=-1&screen_name=dschobel")
     consumer.sign(request)
     val client = new DefaultHttpClient()
     val response = client.execute(request)
     val entity = response.getEntity();
 
     //println(response.getStatusLine().getStatusCode())
     var respMessage = new String()
     if(entity != null){
       respMessage = EntityUtils.toString(entity)
     }
     println(respMessage)
  }
}
