package util

import org.junit._
import org.junit.Assert._

class TryOTest {

  @Test
  def testApply(): Unit = {
    def exceptionableFn: Int = throw new Exception("Test")
    def valueFn: Int = 5

    assertEquals(
      "value functions should return Some(value)",
      TryO { valueFn },
      Some(5)
    )
    assertEquals(
      "exceptionable functions should return None",
      TryO { exceptionableFn },
      None
    )
  }

  @Test
  def testToInt(): Unit = {
    assertEquals("int string should be Some(int)", TryO.toInt("150"), Some(150))
    assertEquals("non-int string should be None", TryO.toInt("X"), None)
  }

  @Test
  def testToLong(): Unit = {
    assertEquals("long string should be Some(long)", TryO.toLong("100000"), Some(100000L))
    assertEquals("non-long string should be None", TryO.toLong("LONG"), None)
  }

}
