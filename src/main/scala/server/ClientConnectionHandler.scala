package server

import events.EventPublishParams
import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, Socket}
import scala.collection.concurrent.{Map => ConcurrentMap, TrieMap}
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import util.TryO

class ClientConnectionHandler(
  port: Int,
  userSocketMap: ConcurrentMap[Long, Socket]
) {

  lazy val serverSocket: ServerSocket = new ServerSocket(port)

  def startF: Future[Unit] = {
    Future {
      while (true) {
        val clientSocket = serverSocket.accept()
        val bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream))
        val clientString = bufferedReader.readLine()
        addConnection(clientString, clientSocket)
      }
    }
  }

  def addConnection(userIdStr: String, socket: Socket): Unit = {
    TryO.toLong(userIdStr).foreach(addConnection(_, socket))
  }

  def addConnection(userId: Long, socket: Socket): Unit = {
    val replacedSocketOpt = {
      val replaced = userSocketMap.replace(userId, socket)
      replaced.foreach(_.close())
      replaced
    }
    if (replacedSocketOpt.isEmpty) {
      userSocketMap += (userId -> socket)
    }
  }

  def closeConnection(userId: Long): Unit = {
    userSocketMap.get(userId).foreach(socket => {
      TryO { socket.close() }
    })
    userSocketMap -= userId
  }

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
        case _: Exception => closeConnection(userId)
      }
    }
  }

  def cleanup(): Unit = {
    for {
      (userId, socket) <- userSocketMap
    } yield {
      TryO { socket.close() }
    }
    userSocketMap.clear()
  }

}
