package freedomandy.service

import akka.actor.Actor
import freedomandy.client.AzureStorageClient
import freedomandy.exception.BaseException
import freedomandy.protocol._
import org.slf4j.LoggerFactory

/**
  * Created by andy on 09/06/2017.
  */
class ContainerActor extends Actor {
  val log = LoggerFactory.getLogger(this.getClass)
  val PROTOCOL = "http"
  val config = freedomandy.config.StorageModule.getConfig
  val ACCOUNT_NAME = config.storageName
  val ACCOUNT_KEY = config.storageKey
  val azureClient = AzureStorageClient(PROTOCOL, ACCOUNT_NAME, ACCOUNT_KEY)

  override def preStart() = {
    log.info("Container actor Start")
  }

  override def postStop() {
    log.info("Container Actor stop")
  }

  private def ExceptionHandling(executeFunction: => ContainerResultMessage): Any = {
    try {
      executeFunction
    } catch {
      case e: BaseException =>
        e
      case e: Throwable =>
        log.info(s"error: ${e.getMessage}")
        BaseException("Internal Error")
    }
  }

  def receive: Receive = {
    case CreateContainerRequest(containerName) =>
      sender ! ExceptionHandling(createContainer(containerName))
      context.stop(self)
    case ListContainersRequest() =>
      sender ! ExceptionHandling(listContainers())
      context.stop(self)
    case DeleteContainerRequest(containerName) =>
      sender ! ExceptionHandling(deleteContainer(containerName))
      context.stop(self)
  }

  def createContainer(containerName: String) =
    CreateContainerResult(azureClient.createContainer(containerName))

  def listContainers() = {
    ListContainersResult(azureClient.listContainers())
  }

  def deleteContainer(containerName: String) = {
    DeleteContainerResult(azureClient.deleteContainer(containerName))
  }
}