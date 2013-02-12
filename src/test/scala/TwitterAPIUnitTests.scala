import org.scalatest.FunSpec

class TwitterAPIUnitTests extends FunSpec {
  describe("parseIds"){
    it("should return an empty list for empty string input"){
      assert(TwitterAPI.parseIds("") === Nil)
    }

    it("should return an empty list for malformed jason"){
      assert(TwitterAPI.parseIds(""" not{really} "json} """) === Nil)
    }

    it("should return a map containing the id fields for well-formed json"){
      assert(TwitterAPI.parseIds("""  {"ids": [12,34]}   """) === List(12,34))
    }
  }
}