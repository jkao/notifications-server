package server

import events.EventProcessor
import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, Socket}
import java.util.logging.Logger
import scala.collection.concurrent.{Map => ConcurrentMap, TrieMap}
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import util.TryO

/*
 * Class that handles interactions with the event source socket.
 * @constructor create new instance of an EventConnectionHandler
 * @param port port to handle event connections
 * @param eventProcessor EventProcessor object to handle incoming events from event source
 * @param loggingOpt pass in an optional logging device for debugging
 */
class EventConnectionHandler(
  port: Int,
  eventProcessor: EventProcessor,
  loggingOpt: Option[Logger] = None
) {

  lazy val serverSocket: ServerSocket = new ServerSocket(port)

  /** Start instance of event connection handler in new Future  */
  def startF: Future[Unit] = {
    Future {
      try {
        while (true) {
          val clientSocket = serverSocket.accept()
          val bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream))
          var rawLine = bufferedReader.readLine()
          while (rawLine != null) {
            Option(rawLine).foreach(eventProcessor.process)
            rawLine = bufferedReader.readLine()
          }
          eventProcessor.cleanup
        }
      } catch {
        case e: Exception => {
          loggingOpt.foreach(_.warning(s"Exception in EventConnectionHandler: ${e.getMessage}"))
          throw e
        }
      } finally {
        loggingOpt.foreach(_.info("Closing EventConnectionHandler"))
        TryO { serverSocket.close() }
      }
    }
  }

}
