package com.auginte.scarango

import akka.actor.{Actor, ActorSystem, Props}
import com.auginte.scarango.common.TestKit
import com.auginte.scarango.errors.{UnexpectedResponse, ScarangoError}
import com.auginte.scarango.request._
import com.auginte.scarango.response._

/**
 * Class for faster testing
 */
object Main extends App {
  class Client extends Actor {

    val dbName = TestKit.unique
    val collectionName = TestKit.unique
    val documentData = """{"some":"data"}"""
    var newDocumentId: String = ""

    def ok(text: String) = println(Console.GREEN_B + "[OK]" + Console.RESET + " " + Console.BLUE + text + Console.RESET)

    def error(text: String) = println(Console.RED_B + "[ERROR]" + Console.RESET + " " + Console.RED + text + Console.RESET)

    def lastRequest(text: String) = println(Console.RED_B + "[LAST REQUEST]" + Console.RESET + " " + Console.RED + text + Console.RESET)

    def unexpected(text: String) = println(Console.RED_B + "[UNEXPECTED]" + Console.RESET + " " + Console.RED + text + Console.RESET)

    val db = system.actorOf(Props[Scarango])

    override def receive: Receive = {
      case "start" =>
        db ! GetVersion
        db ! CreateDatabase(dbName)
        db ! CreateCollection(collectionName, dbName)
        db ! GetCollection(collectionName, dbName)
        db ! CreateDocument(documentData, collectionName, dbName)

      case "removeDocument" =>
        db ! RemoveDocument(newDocumentId, dbName)

      case "cleanup" =>
        db ! RemoveCollection(collectionName, dbName)
        db ! request.Identifiable(ListDatabases, id = "with database")
        db ! RemoveDatabase(dbName)
        db ! request.Identifiable(ListDatabases, id = "database removed")

      case v: Version =>
        ok("Got version: " + v.version)

      case d: DatabaseCreated =>
        ok("Created: " + d.name)

      case response.Identifiable(d: DatabaseList, id, _, _, _) if id == "with database" =>
        ok("Got Databases: " + d.result.mkString(", "))

      case response.Identifiable(d: DatabaseList, id, _, _, _) if id == "database removed" =>
        ok("Got updated Databases: " + d.result.mkString(", "))
        context.system.shutdown()

      case DatabaseRemoved(RemoveDatabase(name), _) =>
        ok("Removed database: " + name)

      case c: CollectionCreated =>
        ok("Collection created: " + c.name + " with id " + c.id)

      case c: Collection =>
        ok(s"Collection: {ID: ${c.id} NAME: ${c.name} STATUS: ${c.enumStatus} TYPE: ${c.enumType}}")

      case c: DocumentCreated =>
        ok("Document created: " + c.id)
        newDocumentId = c.id
        db ! GetDocument(c.id, dbName)
        db ! ListDocuments(collectionName, dbName)

      case d: Document =>
        ok(s"Document ID: ${d.id} DATA: ${d.json}")

      case c: DocumentList =>
        ok("Documents: " + c.ids.mkString(", "))
        self ! "removeDocument"
        self ! "cleanup"

      case c: DocumentRemoved =>
        ok("Document removed: " + c.id + " in " + c.database)

      case CollectionRemoved(RemoveCollection(name, _), raw) =>
        ok("Collection removed: " + name + " with id " + raw.id)

      case e: UnexpectedResponse =>
        error(e.getMessage)
        lastRequest(e.lastRequest.toString)
        context.system.shutdown()

      case e: ScarangoError =>
        error(e.getMessage)
        context.system.shutdown()

      case other =>
        unexpected(other.toString)
        context.system.shutdown()
    }
  }

  implicit val system = ActorSystem("Scarango")
  val client = system.actorOf(Props[Client])
  client ! "start"

}
