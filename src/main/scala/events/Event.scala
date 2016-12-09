package events

/** Represents SoundCloud users to notify from event */
sealed abstract class NotificationTargets

case class NotifySome(targets: Seq[Long]) extends NotificationTargets
case object NotifyNone extends NotificationTargets
case class NotifyFollowers(from: Long) extends NotificationTargets
case object NotifyAll extends NotificationTargets

/** Represents a SoundCloud event */
sealed abstract class Event {
  def payload: String
  def sequenceNumber: Long
  def notificationTargets: NotificationTargets
}

case class FollowEvent(
  from: Long,
  to: Long,
  override val payload: String,
  override val sequenceNumber: Long
) extends Event {
  override val notificationTargets: NotificationTargets = NotifySome(Vector(to))
}

case class UnfollowEvent(
  from: Long,
  to: Long,
  override val payload: String,
  override val sequenceNumber: Long
) extends Event {
  override val notificationTargets: NotificationTargets = NotifyNone
}

case class PrivateMessage(
  from: Long,
  to: Long,
  override val payload: String,
  override val sequenceNumber: Long
) extends Event {
  override val notificationTargets: NotificationTargets = NotifySome(Vector(to))
}

case class StatusUpdate(
  from: Long,
  override val payload: String,
  override val sequenceNumber: Long
) extends Event {
  override val notificationTargets: NotificationTargets = NotifyFollowers(from)
}

case class Broadcast(
  override val payload: String,
  override val sequenceNumber: Long
) extends Event {
  override val notificationTargets: NotificationTargets = NotifyAll
}

