package events

import util.TryO

object EventParser {

  def parse(event: String): Option[Event] = {
    val splitString = event.split('|')
    splitString match {
      case Array(seqStr, "F", fromStr, toStr) => {
        for {
          seqId <- TryO.toLong(seqStr)
          from <- TryO.toLong(fromStr)
          to <- TryO.toLong(toStr)
        } yield {
          FollowEvent(from, to, event, seqId)
        }
      }
      case Array(seqStr, "U", fromStr, toStr) => {
        for {
          seqId <- TryO.toLong(seqStr)
          from <- TryO.toLong(fromStr)
          to <- TryO.toLong(toStr)
        } yield {
          UnfollowEvent(from, to, event, seqId)
        }
      }
      case Array(seqStr, "B") => {
        for {
          seqId <- TryO.toLong(seqStr)
        } yield {
          Broadcast(event, seqId)
        }
      }
      case Array(seqStr, "P", fromStr, toStr) => {
        for {
          seqId <- TryO.toLong(seqStr)
          from <- TryO.toLong(fromStr)
          to <- TryO.toLong(toStr)
        } yield {
          PrivateMessage(from, to, event, seqId)
        }
      }
      case Array(seqStr, "S", fromStr) => {
        for {
          seqId <- TryO.toLong(seqStr)
          from <- TryO.toLong(fromStr)
        } yield {
          StatusUpdate(from, event, seqId)
        }
      }
      case _ => None
    }
  }

}
