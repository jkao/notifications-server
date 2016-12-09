package server

import events.EventProcessor
import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, Socket}
import java.util.logging.Logger;
import scala.collection.concurrent.{Map => ConcurrentMap, TrieMap}
import scala.collection.mutable.StringBuilder
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import util.TryO

/*
 * Represents an instance of a server
 * @constructor create new instance of NotificationsServer
 * @param eventConnectionHandler event connection handler for the event source
 * @param clientConnectionHandler client connection handler for client sources
 */
class NotificationsServer(
  eventConnectionHandler: EventConnectionHandler,
  clientConnectionHandler: ClientConnectionHandler
) {
  /** Start instance of server */
  def startF: Future[Unit] = {
    val eventSourceF = eventConnectionHandler.startF
    val clientConnectionsF = clientConnectionHandler.startF
    for {
      eventSource <- eventSourceF
      clientConnections <- clientConnectionsF
    } yield {}
  }
}

/** Main server entry point */
object NotificationsServer {

  val logger = Logger.getLogger("NotificationsServer")

  private def parseArgs(args: Array[String]): Map[String, String] = {
    (for {
      argPairs <- args.grouped(2)
      key <- argPairs.lift(0)
      value <- argPairs.lift(1)
    } yield {
      key -> value
    }).toMap
  }

  /*
   * java -jar notification-server.jar
   *    --eventPort $EVENT_PORT
   *    --clientPort $CLIENT_PORT
   *    --sortWindow $SORT_WINDOW
   */
  def main(args: Array[String]): Unit = {
    val argsMap = parseArgs(args)

    val eventPort = argsMap.get("--eventPort").flatMap(TryO.toInt).getOrElse(9090)
    val clientPort = argsMap.get("--clientPort").flatMap(TryO.toInt).getOrElse(9099)
    val sortWindow = argsMap.get("--sortWindow").flatMap(TryO.toInt).getOrElse(250)

    val clientConnectionHandler = new ClientConnectionHandler(clientPort, Some(logger))
    val eventProcessor =
      new EventProcessor(
        publishFn = clientConnectionHandler.publishToConnections(_),
        publishCleanupFn = () => clientConnectionHandler.cleanup(),
        sortWindow = sortWindow
      )
    val eventConnectionHandler = new EventConnectionHandler(eventPort, eventProcessor, Some(logger))
    val server = new NotificationsServer(eventConnectionHandler, clientConnectionHandler)

    val startF = server.startF

    logger.info(s"Server starting")
    logger.info(s"Event port: $eventPort")
    logger.info(s"Client port: $clientPort")
    logger.info(s"Sort window size: $sortWindow")

    Await.ready(startF, Duration.Inf)
  }

}
