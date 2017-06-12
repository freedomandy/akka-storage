package freedomandy.service

import akka.actor.Actor
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.directives.{FileInfo => DFileInfo}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import freedomandy.client.{AzureStorageClient, AzureStorageRestClient}
import freedomandy.data.{BlobInfo, BlobUploadInfo}
import freedomandy.exception.BaseException
import freedomandy.protocol._
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by andy on 05/06/2017.
  */
class BlobActor extends Actor {
  val config = freedomandy.config.StorageModule.getConfig
  val PROTOCOL = "http"
  val ACCOUNT_NAME = config.storageName
  val ACCOUNT_KEY = config.storageKey
  val azureClient = AzureStorageClient(PROTOCOL, ACCOUNT_NAME, ACCOUNT_KEY)
  val DOWNLOAD_URL_PREFIX = "http://localhost:8080/apis/v1/"
  val log = LoggerFactory.getLogger(this.getClass)
  val http = Http(context.system)
  implicit val materializer = ActorMaterializer()

  private def ExceptionHandling(executeFunction: => BlobResultMessage): Any = {
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

  override def preStart() = {
    log.info("Blob Actor Starting")
  }

  override def postStop() = {
    log.info("Blob Actor Stopping")
  }

  def receive = {
    case UploadBlobRequest(container, metaData, byteSource) =>
      val theSender = sender
      saveBlob(container, metaData, byteSource).onComplete {
        case Success(result) =>
          theSender ! result
          context.stop(self)
        case Failure(err) => err match {
          case e: BaseException =>
            theSender ! e
            context.stop(self)
          case t: Throwable =>
            log.info(s"Error: ${t.toString}")
            theSender ! BaseException("Failed to upload file")
            context.stop(self)
        }
      }(context.dispatcher)
    case ListBlobRequest(container) =>
      sender ! ExceptionHandling(listBlobs(container))
      context.stop(self)
    case GetBlobRequest(container, blob) =>
      sender ! ExceptionHandling(getBlob(container, blob))
      context.stop(self)
    case DeleteBlobRequest(container, blob) =>
      sender ! ExceptionHandling(deleteBlob(container, blob))
      context.stop(self)
  }

  def saveBlob(containerName: String, metaData: DFileInfo, byteSource: Source[ByteString, Any]): Future[UploadBlobResult] = {
    implicit val executeContext = context.system.dispatchers.lookup("client-handler-dispatcher")

    case class Chunk(byteCount: Int = 0, byteString: ByteString = ByteString.empty)

    val MAX_CHUNK_SIZE: Int = 4096000
    var blockList: List[String] = Nil

    def isEndOfChunk(chunk: Chunk, data: ByteString):Boolean =
      chunk.byteCount + data.length > MAX_CHUNK_SIZE

    def chunkUpload(sasToken: String)(chunkState: Chunk, byteString: ByteString): Chunk = {
      if (isEndOfChunk(chunkState, byteString)) {
        val blockId = putBlock(chunkState.byteString, sasToken)
        blockList = blockList ::: List(blockId)

        Chunk(byteString.length, byteString)
      } else {
        Chunk(byteCount = chunkState.byteCount + byteString.length,
          byteString = chunkState.byteString ++ byteString)
      }
    }

    def putBlock(chunk: ByteString, sasToken: String): String = {
      val payLoad: Array[Byte] = chunk.toArray
      val blockId: String = java.util.Base64.getEncoder().encodeToString(java.util.UUID.randomUUID().toString.getBytes)
      val destination = sasToken + "&comp=block&blockid=" + blockId

      AzureStorageRestClient.putBlock(destination, payLoad)

      blockId
    }

    def commitBlockList(sasToken: String, blockIds: List[String], blobType: String = "application/octet-stream"): Future[HttpResponse] = {
      val destination = sasToken + "&comp=blocklist"
      val body =
        blockIds.fold("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<BlockList>")(_ + "<Latest>" + _ + "</Latest>") +
          "</BlockList>"

      http.singleRequest(HttpRequest(
        method = HttpMethods.PUT,
        uri = destination,
        entity = HttpEntity(body)).withHeaders(List(RawHeader("x-ms-blob-content-type", blobType))))(materializer)
    }

    val fileName = metaData.fileName
    val fileType = metaData.getContentType.toString()
    val uploadUrl = azureClient.getUploadBlobSasUri(containerName, fileName, 2)

    for {
      lastChunk <- byteSource.runFold(Chunk())(chunkUpload(uploadUrl))(materializer)
      commitRes <- {
        blockList = blockList ::: List(putBlock(lastChunk.byteString, uploadUrl))
        log.info(s"blockList: ${blockList}")
        commitBlockList(uploadUrl, blockList, fileType)
      }
      result <- Future {
        log.info("Save file successfully")
        commitRes.entity.discardBytes(materializer)
        val fileSize = (blockList.length - 1) * MAX_CHUNK_SIZE + lastChunk.byteString.length
        UploadBlobResult(BlobUploadInfo(containerName, fileName, fileType, fileSize, DOWNLOAD_URL_PREFIX +
          s"container/$containerName/file/$fileName"))
      }
    } yield result
  }

  def listBlobs(container: String): ListBlobResult = {
      ListBlobResult(azureClient.listBlobSas(container, "RL", 2))
  }

  def getBlob(containerName: String, blobName: String): GetBlobResult = {
    GetBlobResult(BlobInfo(blobName, azureClient.getBlobSas(containerName, blobName, "RL" , 2)))
  }

  def deleteBlob(containerName: String, blobName: String): DeleteBlobResult = {
    DeleteBlobResult(azureClient.deleteBlob(containerName, blobName))
  }

}
