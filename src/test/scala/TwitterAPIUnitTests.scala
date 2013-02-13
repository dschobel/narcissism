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
      assert(parse("""  {"ids": [12,34]}   """) === List(12,34))
    }
  }
}