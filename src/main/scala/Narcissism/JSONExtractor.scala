package narcissism

/**
 * User: Daniel Schobel
 * Date: 18/02/13
 * Time: 12:30 PM
 */
object JSONExtractor {
  class CC[T] { def unapply(a:Any):Option[T] = Some(a.asInstanceOf[T]) }

  object M extends CC[Map[String, Any]]
  object L extends CC[List[Any]]
  object S extends CC[String]
  object D extends CC[Double]
  object B extends CC[Boolean]
}