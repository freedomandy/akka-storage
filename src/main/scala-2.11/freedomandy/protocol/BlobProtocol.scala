package freedomandy.protocol

import akka.stream.scaladsl.Source
import akka.util.ByteString
import freedomandy.data.{BlobInfo, BlobUploadInfo}
import akka.http.scaladsl.server.directives.{FileInfo => DFileInfo}

/**
  * Created by andy on 05/06/2017.
  */
sealed trait BlobRequestMessage extends RequestMessage
case class UploadBlobRequest(container: String, metaData: DFileInfo, byteSource: Source[ByteString, Any]) extends BlobRequestMessage
case class ListBlobRequest(container: String) extends BlobRequestMessage
case class GetBlobRequest(container: String, file: String) extends BlobRequestMessage
case class DeleteBlobRequest(container: String, file: String) extends BlobRequestMessage

sealed trait BlobResultMessage extends ResultMessage
case class UploadBlobResult(fileInfo: BlobUploadInfo) extends BlobResultMessage
case class ListBlobResult(blobInfos: List[BlobInfo]) extends BlobResultMessage
case class GetBlobResult(fileInfo: BlobInfo) extends BlobResultMessage
case class DeleteBlobResult(isDeleted: Boolean) extends BlobResultMessage
