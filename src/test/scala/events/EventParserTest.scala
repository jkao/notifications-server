package events

import org.junit._
import org.junit.Assert._

class EventParserTest {

  @Test
  def testFollowEvent() {
    val result1 = EventParser.parse("120|F|6|5")
    val expected1 = Some(FollowEvent(6, 5, "120|F|6|5", 120))
    assertEquals("Proper follow event format should parse", result1, expected1)

    val result2 = EventParser.parse("120|F|6 |5")
    val expected2 = None
    assertEquals("Bad follow event shouldn't parse", result2, expected2)
  }

  @Test
  def testUnfollowEvent() {
    val result1 = EventParser.parse("33|U|18|19000")
    val expected1 = Some(UnfollowEvent(18, 19000, "33|U|18|19000", 33))
    assertEquals("Proper unfollow event format should parse", result1, expected1)

    val result2 = EventParser.parse("33|UX|18|19000")
    val expected2 = None
    assertEquals("Bad unfollow event shouldn't parse", result2, expected2)
  }

  @Test
  def testBroadcastEvent() {
    val result1 = EventParser.parse("100|B")
    val expected1 = Some(Broadcast("100|B", 100))
    assertEquals("Proper broadcast event format should parse", result1, expected1)

    val result2 = EventParser.parse("100|B|9")
    val expected2 = None
    assertEquals("Bad broadcast event shouldn't parse", result2, expected2)
  }

  @Test
  def testPrivateMessageEvent() {
    val result1 = EventParser.parse("28|P|29|1000")
    val expected1 = Some(PrivateMessage(29, 1000, "28|P|29|1000", 28))
    assertEquals("Proper PM event format should parse", result1, expected1)

    val result2 = EventParser.parse("28|PM|120|19000")
    val expected2 = None
    assertEquals("Bad PM event shouldn't parse", result2, expected2)
  }

  @Test
  def testStatusUpdateEvent() {
    val result1 = EventParser.parse("28|S|600")
    val expected1 = Some(StatusUpdate(600, "28|S|600", 28))
    assertEquals("Proper status update event format should parse", result1, expected1)

    val result2 = EventParser.parse("28|S|100|1")
    val expected2 = None
    assertEquals("Bad status update event shouldn't parse", result2, expected2)
  }

}
