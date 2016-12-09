package events

import org.junit._
import org.junit.Assert._
import scala.collection.immutable.VectorBuilder

class EventProcessorTest {

  val voidPublishFn = (e: EventPublishParams) => ()
  val voidPublishCleanupFn = () => ()

  def sinkPublishFn(sink: VectorBuilder[EventPublishParams])(e: EventPublishParams): Unit = {
    sink += e
  }

  @Test
  def testHeapNotFull(): Unit = {
    def noPublishAllowedFn(e: EventPublishParams) = {
      fail("No publish allowed")
    }

    val epp =
      new EventProcessor(
        noPublishAllowedFn,
        voidPublishCleanupFn,
        sortWindow = 2
      )
    epp.process("120|F|6|5")
  }

  @Test
  def testEventsPublishSortedOnFull(): Unit = {
    def assertSeq1Published(e: EventPublishParams) = {
      assertEquals(e.userIds, Vector(4L))
      assertEquals(e.payload, "1|F|3|4")
      assertFalse(e.broadcast)
    }
    val epp =
      new EventProcessor(
        assertSeq1Published,
        voidPublishCleanupFn,
        sortWindow = 2
      )
    epp.process("2|F|6|5")
    epp.process("1|F|3|4")
  }

  @Test
  def testAssertFollowUnfollow(): Unit = {
    val sink = new VectorBuilder[EventPublishParams]()
    val sinkFn = sinkPublishFn(sink)(_)

    val epp =
      new EventProcessor(
        sinkFn,
        voidPublishCleanupFn,
        sortWindow = 10
      )

    epp.process("4|S|2")
    epp.process("3|U|1|2")
    epp.process("2|S|2")
    epp.process("1|F|1|2")

    // flush out publishes
    epp.cleanup

    // verify publish order events and no publishes when no follows exist
    val results = sink.result()
    assertEquals(
      Vector(
        EventPublishParams(Vector(2), "1|F|1|2", false),
        EventPublishParams(Vector(1), "2|S|2", false)
      ),
      results
    )
  }

  @Test
  def testBroadcast(): Unit = {
    val sink = new VectorBuilder[EventPublishParams]()
    val sinkFn = sinkPublishFn(sink)(_)

    val epp =
      new EventProcessor(
        sinkFn,
        voidPublishCleanupFn,
        sortWindow = 10
      )
    epp.process("4|B")

    // flush out publishes
    epp.cleanup

    // verify broadcast gets sent
    val results = sink.result()
    assertEquals(
      Vector(EventPublishParams(Vector.empty, "4|B", true)),
      results
    )
  }

  @Test
  def testPrivateMessage(): Unit = {
    val sink = new VectorBuilder[EventPublishParams]()
    val sinkFn = sinkPublishFn(sink)(_)

    val epp =
      new EventProcessor(
        sinkFn,
        voidPublishCleanupFn,
        sortWindow = 3
      )

    // send PMs out of order
    epp.process("1|P|5|1")
    epp.process("100|P|1|8")
    epp.process("2|P|6|1")
    epp.process("101|P|1|9") // sortWindow cuts this one out
    epp.process("50|P|10|20")
    epp.process("102|P|5|6") // sortWindow cuts this one out

    // verify PMs gets sent to the right users in-order
    val results = sink.result()
    assertEquals(
      Vector(
        EventPublishParams(Vector(1), "1|P|5|1", false),
        EventPublishParams(Vector(1), "2|P|6|1", false),
        EventPublishParams(Vector(20), "50|P|10|20", false),
        EventPublishParams(Vector(8), "100|P|1|8", false)
      ),
      results
    )
  }

}
