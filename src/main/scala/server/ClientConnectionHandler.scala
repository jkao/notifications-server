package server

import events.EventPublishParams
import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, Socket}
import java.util.logging.Logger
import scala.collection.concurrent.{Map => ConcurrentMap, TrieMap}
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import util.TryO

/*
 * Class that handles interactions with client socket sources.
 * @constructor create new instance of a ClientConnectionHandler
 * @param port port to handle client connections
 * @param loggingOpt pass in an optional logging device for debugging
 */
class ClientConnectionHandler(port: Int, loggingOpt: Option[Logger] = None) {

  lazy val userSocketMap: ConcurrentMap[Long, Socket] = new TrieMap[Long, Socket]()
  lazy val serverSocket: ServerSocket = new ServerSocket(port)

  /** Start instance of client connection handler in new Future  */
  def startF: Future[Unit] = {
    Future {
      try {
        while (true) {
          val clientSocket = serverSocket.accept()
          val bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream))
          val clientString = bufferedReader.readLine()
          addConnection(clientString, clientSocket)
        }
      } finally {
        cleanup()
      }
    }
  }

  private def addConnection(userIdStr: String, socket: Socket): Unit = {
    TryO.toLong(userIdStr).foreach(addConnection(_, socket))
  }

  private def addConnection(userId: Long, socket: Socket): Unit = {
    val replacedSocketOpt = {
      val replaced = userSocketMap.replace(userId, socket)
      replaced.foreach(_.close())
      replaced
    }
    if (replacedSocketOpt.isEmpty) {
      userSocketMap += (userId -> socket)
    }
  }

  private def closeConnection(userId: Long): Unit = {
    userSocketMap.get(userId).foreach(socket => {
      TryO { socket.close() }
    })
    userSocketMap -= userId
  }

  /*
   * Method to publish payloads to connected clients
   * @param params params specifying who to target and what to broadcast
   */
  def publishToConnections(params: EventPublishParams): Unit = {
    val targets =
      if (params.broadcast) {
        userSocketMap.keys.toVector
      } else {
        params.userIds
      }
    for {
      userId <- targets
      socket <- userSocketMap.get(userId)
    } {
      try {
        val eolBytes = "\r\n".getBytes
        val outputStream = socket.getOutputStream()
        outputStream.write(params.payload.getBytes)
        outputStream.write(eolBytes)
        outputStream.flush()
      } catch {
        case e: Exception => {
          loggingOpt.foreach(_.warning(s"Exception in ClientConnectionHandler: ${e.getMessage}"))
          loggingOpt.foreach(_.warning(s"Closing connection for $userId"))
          closeConnection(userId)
        }
      }
    }
  }

  /** Method to cleanup connections handled by this instance.
   *  Call this when finishing up with this instance. */
  def cleanup(): Unit = {
    for {
      (userId, socket) <- userSocketMap
    } yield {
      TryO { socket.close() }
    }
    userSocketMap.clear()
  }

}
