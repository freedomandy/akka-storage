package freedomandy.route

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import akka.util.Timeout
import freedomandy.data.ErrorInfo
import freedomandy.exception.BaseException
import freedomandy.protocol._
import freedomandy.service.ContainerActor
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

/**
  * Created by andy on 10/06/2017.
  */
case class ContainerRoute(actorSystem: ActorSystem) extends Directives with CustomJsonSupport {
  implicit val system = actorSystem
  implicit val ec = system.dispatchers.lookup("response-handler-dispatcher")

  val containerLog = LoggerFactory.getLogger(this.getClass)
  val exceptionHandler = ExceptionHandler {
    case e: BaseException =>
      complete(e.httpStatusCode, ErrorInfo(e.errorCode, e.errorMsg))
    case e: Throwable =>
      containerLog.info(s"error: ${e.getMessage}")
      complete(500, ErrorInfo("00-000", "Internal Error"))
  }

  def route = pathPrefix("apis" / "v1") {
    handleExceptions(exceptionHandler) {
      path("container" / Segment) { containerName =>
        post {
          implicit val timeout: Timeout = 12.seconds
          val result = (system.actorOf(Props[ContainerActor]) ? CreateContainerRequest(containerName)).map {
            case CreateContainerResult(containerInfo) => containerInfo
            case e: BaseException => throw e
            case _ => throw BaseException("Internal Error")
          }

          complete(201, result)
        } ~ delete {
          implicit val timeout: Timeout = 12.seconds
          val result = (system.actorOf(Props[ContainerActor]) ? DeleteContainerRequest(containerName)).map {
            case DeleteContainerResult(isDeleted) => isDeleted
            case e: BaseException => throw e
            case _ => throw BaseException("Internal Error")
          }

          onSuccess(result) {
            case true => complete(204, null)
            case false => complete(304, null)
          }
        }
      } ~ path("containers") {
        get {
          implicit val timeout: Timeout = 12.seconds
          val result = (system.actorOf(Props[ContainerActor]) ? ListContainersRequest()).mapTo[ListContainersResult].
            map(result => result.containers)

          complete(200, result)
        }
      }
    }
  }
}
