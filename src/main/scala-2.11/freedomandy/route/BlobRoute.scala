package freedomandy.route

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.headers.ContentDispositionTypes.attachment
import akka.http.scaladsl.model.headers.{`Content-Disposition`, `Content-Type`}
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, HttpResponse, Multipart}
import akka.http.scaladsl.model.Multipart.BodyPart
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Framing}
import akka.util.{ByteString, Timeout}
import akka.pattern.ask
import freedomandy.data.{BlobInfo, BlobUploadInfo, ErrorInfo}
import freedomandy.exception.BaseException
import freedomandy.protocol.{BlobResultMessage, ListBlobRequest, _}
import freedomandy.service.BlobActor
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by andy on 05/06/2017.
  */
case class BlobRoute(actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) extends Directives with CustomJsonSupport {
  implicit val system = actorSystem
  implicit val materializer = actorMaterializer
  implicit val ec = system.dispatchers.lookup("response-handler-dispatcher")

  val fileLog = LoggerFactory.getLogger(this.getClass)
  val exceptionHandler = ExceptionHandler {
    case e: BaseException =>
      complete(e.httpStatusCode, ErrorInfo(e.errorCode, e.errorMsg))
    case e: Throwable =>
      fileLog.info(s"error: ${e.toString}")
      complete(500, ErrorInfo("00-000", "Internal Error"))
  }

  def route =
    pathPrefix("apis" / "v1") {
      path("container" / Segment / "file") { container =>
        handleExceptions(exceptionHandler) {
          pathEnd {
            post {
              implicit val timeout: Timeout = 1.hour
              fileUpload("file") {
                case (metadata, byteSource) =>
                  fileLog.debug("file upload request")
                  val result = (system.actorOf(Props[BlobActor]) ? UploadBlobRequest(container, metadata, byteSource)).
                    map {
                      case UploadBlobResult(blobInfo) => blobInfo
                      case e: BaseException => throw e
                      case _ => throw BaseException("Internal Error")
                    }

                  complete(200, result)
              }
            }
          }
        }
      } ~ path("container" / Segment) { container =>
        handleExceptions(exceptionHandler) {
          pathEnd {
            get {
              implicit val timeout: Timeout = 12.second
              val result = (system.actorOf(Props[BlobActor]) ? ListBlobRequest(container)).map {
                  case ListBlobResult(list) => list
                  case e: BaseException => throw e
                  case _ => throw BaseException("Internal Error")
                }

              complete(200, result)
            }
          }
        }
      } ~ path("container" / Segment / "file" / Segment) { (container,file) =>
        handleExceptions(exceptionHandler) {
          get {
            implicit val timeout: Timeout = 12.seconds
            val fileSource = (system.actorOf(Props[BlobActor]) ? GetBlobRequest(container, file)).map {
              case GetBlobResult(blobInfo) =>
                Http().singleRequest(HttpRequest(uri = blobInfo.sasToken)).map(res =>
                  res.copy(200, List(`Content-Disposition`(attachment, Map("filename" -> blobInfo.fileName)))))
              case e: BaseException => throw e
              case _ => throw BaseException("Internal Error")
            }

            complete(fileSource)
          } ~ delete {
            implicit val timeout: Timeout = 12.seconds
            val result = (system.actorOf(Props[BlobActor]) ? DeleteBlobRequest(container, file)).map {
              case DeleteBlobResult(isDeleted) => isDeleted
              case e: BaseException => throw e
              case _ => throw BaseException("Internal Error")
            }

            onSuccess(result) {
              case true => complete(204, null)
              case false => complete(304, null)
            }
          }
        }
      }
    }
}
