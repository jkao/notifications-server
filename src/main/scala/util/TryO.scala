package util

/** Helper to convert Exception'able cases to Option */
object TryO {

  /*
   * Process an exceptionable function
   * @param f function that may or may not throw an exception
   */
  def apply[T](f: => T): Option[T] = {
    try {
      Some(f)
    } catch {
      case _: Exception => None
    }
  }

  /*
   * Parse a string to an integer
   * @param string string to parse
   */
  def toInt(string: String): Option[Int] = {
    TryO { string.toInt }
  }

  /*
   * Parse a string to a long
   * @param string string to parse
   */
  def toLong(string: String): Option[Long] = {
    TryO { string.toLong }
  }

}
