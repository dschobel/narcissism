package narcissism

import org.squeryl.Schema
import java.sql.Timestamp

/**
 * Daniel Schobel
 * Date: 21/02/13
 * Time: 7:26 PM
 */
class Author(val id: Long,  val firstName: String,  val lastName: String,  val email: Option[String]) {
  def this() = this(0,"","",Some(""))

}

class UnfollowEvent(val id:Long, val unfollowTime: Timestamp)

object UnfollowEvent{
  def apply(){
    val d =  new java.util.Date()
    val ts = new Timestamp(d.getTime())
    new UnfollowEvent(0, ts)
  }
}


object Unfollows extends Schema{
  val unfollows = table[UnfollowEvent]
}
