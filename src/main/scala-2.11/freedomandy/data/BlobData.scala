package freedomandy.data

/**
  * Created by andy on 05/06/2017.
  */
case class BlobUploadInfo(container: String, fileName: String, fileType: String, size: Long, uri:String)
case class BlobInfo(fileName: String, sasToken: String)
