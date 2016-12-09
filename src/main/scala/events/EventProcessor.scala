package events

import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, Socket}
import java.util.Comparator
import java.util.concurrent.PriorityBlockingQueue
import scala.collection.concurrent.{Map => ConcurrentMap, TrieMap}
import scala.collection.mutable.PriorityQueue
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import util.TryO

case object EventOrder extends Comparator[Event] {
  def compare(e1: Event, e2: Event) = {
    implicitly[Ordering[Long]].compare(e1.sequenceNumber, e2.sequenceNumber)
  }
}

case class EventPublishParams(
  userIds: Vector[Long],
  payload: String,
  broadcast: Boolean
)

class EventProcessor(
  publishFn: (EventPublishParams) => Unit,
  publishCleanupFn: () => Unit,
  sortWindow: Int = 100
) {

  val followersMap: ConcurrentMap[Long, Set[Long]] = new TrieMap[Long, Set[Long]]()
  val heap: PriorityBlockingQueue[Event] = new PriorityBlockingQueue[Event](sortWindow, EventOrder)

  def process(eventStr: String): Unit = {
    EventParser.parse(eventStr).foreach(maybeProcess)
  }

  def cleanup: Unit = {
    while (!heap.isEmpty) {
      process_!(heap.poll)
    }
    followersMap.clear()
    publishCleanupFn()
  }

  private def maybeProcess(event: Event): Unit = {
    heap.offer(event)
    if (heap.size >= sortWindow) {
      process_!(heap.poll)
    }
  }

  private def process_!(event: Event): Unit = {
    event match {
      case (fe: FollowEvent) => {
        val followers = followersMap.getOrElse(fe.to, Set[Long]())
        followersMap += (fe.to -> (followers + fe.from))
      }
      case (ue: UnfollowEvent) => {
        val followers = followersMap.getOrElse(ue.to, Set[Long]())
        followersMap += (ue.to -> (followers - ue.from))
      }
      case _ =>
    }

    event.notificationTargets match {
      case NotifySome(userIds: Vector[Long]) if userIds.nonEmpty => {
        publishFn(EventPublishParams(userIds, event.payload, false))
      }
      case NotifyFollowers(userId: Long) => {
        val followers = followersMap.getOrElse(userId, Set.empty).toVector
        if (followers.nonEmpty) {
          publishFn(EventPublishParams(followers, event.payload, false))
        }
      }
      case NotifyAll => {
        publishFn(EventPublishParams(Vector.empty, event.payload, true))
      }
      case _ =>
    }
  }

}
