package util

/*
 * Helper to convert Exception'able cases to Option
 */
object TryO {

  def apply[T](f: => T): Option[T] = {
    try {
      Some(f)
    } catch {
      case _: Exception => None
    }
  }

  def toInt(string: String): Option[Int] = {
    TryO { string.toInt }
  }

  def toLong(string: String): Option[Long] = {
    TryO { string.toLong }
  }

}
