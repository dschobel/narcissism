import org.scalatest.{BeforeAndAfter, PrivateMethodTester, FunSpec}

class TwitterAPIUnitTests extends FunSpec with PrivateMethodTester with BeforeAndAfter {
  describe("parseIds"){
    val parse_method = PrivateMethod[List[BigDecimal]]('parseIds)
    val parse = (s:String) => TwitterAPI invokePrivate parse_method(s)

    it("should return an empty list for empty string input"){
      assert(parse("") === Nil)
    }

    it("should return an empty list for malformed jason"){
      assert(parse(""" not{really} "json} """) === Nil)
    }

    it("should return a map containing the id fields for well-formed json"){
      val res = parse("""  {"ids": [12,34]}   """)
      assert(res === List("12","34"))
    }
  }

  describe("parseUserResponse"){

    it("should parse users"){
      val json = """[{"id":14959032,"entities":{"url":{"urls":[{"expanded_url":null,"url":"http:\/\/www.2degreesrecruitment.com.au","indices":[0,37],"display_url":null}]},"description":{"urls":[]}},"followers_count":280,"url":"http:\/\/www.2degreesrecruitment.com.au","contributors_enabled":false,"time_zone":"Sydney","profile_background_color":"C0DEED","utc_offset":36000,"verified":false,"default_profile":true,"name":"Diane Cotterill","geo_enabled":false,"lang":"en","profile_background_image_url":"http:\/\/a0.twimg.com\/images\/themes\/theme1\/bg.png","location":"Sydney, Australia","profile_link_color":"0084B4","protected":false,"profile_image_url":"http:\/\/a0.twimg.com\/profile_images\/2537252057\/np4polio0c7woe2m9v2k_normal.jpeg","listed_count":13,"profile_use_background_image":true,"notifications":false,"follow_request_sent":false,"screen_name":"dmcotter","profile_text_color":"333333","profile_image_url_https":"https:\/\/twimg0-a.akamaihd.net\/profile_images\/2537252057\/np4polio0c7woe2m9v2k_normal.jpeg","id_str":"14959032","following":false,"profile_background_image_url_https":"https:\/\/twimg0-a.akamaihd.net\/images\/themes\/theme1\/bg.png","is_translator":false,"profile_sidebar_border_color":"C0DEED","default_profile_image":false,"status":{"entities":{"hashtags":[],"user_mentions":[],"urls":[]},"place":null,"coordinates":null,"retweeted":false,"id_str":"302293857301848064","contributors":null,"in_reply_to_user_id":null,"in_reply_to_status_id":null,"retweet_count":1,"in_reply_to_status_id_str":null,"favorited":false,"in_reply_to_screen_name":null,"text":"They also want front-end developers, iOS developers and Sys Engineers so ping me for more info people of the globe :)\n   2\/2","in_reply_to_user_id_str":null,"geo":null,"truncated":false,"source":"web","id":302293857301848064,"created_at":"Fri Feb 15 05:50:46 +0000 2013"},"created_at":"Fri May 30 23:58:42 +0000 2008","favourites_count":660,"friends_count":540,"profile_background_tile":false,"description":"Founder of 2 degrees recruitment, a referral based recruitment company looking to connect great people in the IT industry. ","profile_sidebar_fill_color":"DDEEF6","statuses_count":690}]"""
      val user: Seq[User] = scala.util.parsing.json.JSON.parseFull(json) match{
        case None => Nil
        case Some(fields: List[Map[String,Any]]) => TwitterAPI.parseUsers(fields)
      }

      assert(user.head.id_str === "14959032")
      assert(user.head.description === "Founder of 2 degrees recruitment, a referral based recruitment company looking to connect great people in the IT industry. ")
      assert(user.head.location === "Sydney, Australia")
      assert(user.head.name === "Diane Cotterill")
      //assert(user.head.profile_image_url === "http:\\/\\/a0.twimg.com\\/profile_images\\/2537252057\\/np4polio0c7woe2m9v2k_normal.jpeg")
      assert(user.head.screen_name === "dmcotter")
    }
  }
}