package freedomandy

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import freedomandy.route.{ContainerRoute, BlobRoute}
import akka.http.scaladsl.server.Directives._

import scala.util.{Failure, Success}

/**
  * Created by andy on 05/06/2017.
  */
object Boot extends App {

  implicit val system = ActorSystem("storage-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val fileRoute = BlobRoute(system, materializer)
  val containerRoute = ContainerRoute(system)

  val bindingFuture = Http().bindAndHandle(fileRoute.route ~ containerRoute.route , "0.0.0.0", 8080)

  bindingFuture.onComplete {
    case Success(binding) ⇒
      println(s"Server is listening on localhost:8080")
    case Failure(e) ⇒
      println(s"Binding failed with ${e.getMessage}")
      system.terminate()
  }

}
