package freedomandy.route

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import freedomandy.data.{ContainerInfo, ErrorInfo, BlobInfo, BlobUploadInfo}
import spray.json.DefaultJsonProtocol

/**
  * Created by andy on 05/06/2017.
  */
trait CustomJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val FileUploadM = jsonFormat5(BlobUploadInfo)
  implicit val FileInfoM = jsonFormat2(BlobInfo)
  implicit val ContainerInfoM = jsonFormat2(ContainerInfo)
  implicit val ErrorInfoM = jsonFormat2(ErrorInfo)

}
