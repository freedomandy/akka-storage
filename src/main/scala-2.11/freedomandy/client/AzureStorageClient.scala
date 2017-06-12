package freedomandy.client

import java.net.URISyntaxException
import java.security.InvalidKeyException
import java.util
import java.util.{Calendar, Date}

import com.microsoft.azure.storage.blob._
import com.microsoft.azure.storage.{CloudStorageAccount, StorageException}
import freedomandy.data.{ContainerInfo, BlobInfo}
import freedomandy.exception.{InvalidInputException, NotFoundException}
import org.slf4j.{Logger, LoggerFactory}


/**
  * Created by andy on 05/06/2017.
  */

class AzureStorageClient(protocol: String, accountName: String, accountKey: String) {
  val log: Logger = LoggerFactory.getLogger(this.getClass)
  var storageAccount: CloudStorageAccount = null
  var blobServiceClient: CloudBlobClient = null

  def init(): Unit = {
    val storageConnection = "DefaultEndpointsProtocol=" + protocol + ";AccountName=" + accountName + ";AccountKey=" +
      accountKey
    try {
      storageAccount = CloudStorageAccount.parse(storageConnection)
      blobServiceClient = storageAccount.createCloudBlobClient()
    } catch {
      case e: StorageException =>
        log.info("Can't connect to Azure storage: {}", e.getMessage)
      case e: InvalidKeyException =>
        log.info("Invalid key: {}", e.getMessage)
      case e: URISyntaxException =>
        log.info("URI syntax error: {}", e.getMessage)
    }
  }

  def getBlobSas(containerName: String, fileName: String, permission: String, numOfHours: Int): String = {
    val containerRef: CloudBlobContainer = blobServiceClient.getContainerReference(containerName)
    val blobRef = containerRef.getBlockBlobReference(fileName)

    if (blobRef.exists()) {
      val sasConstraints: SharedAccessBlobPolicy = new SharedAccessBlobPolicy()
      val calendar = Calendar.getInstance()
      calendar.setTime(new Date())
      calendar.add(Calendar.HOUR, +numOfHours)
      val expiryTime = calendar.getTime
      log.debug("Expiry time: " + expiryTime.toString)

      val permissionSet: util.EnumSet[SharedAccessBlobPermissions] =
        util.EnumSet.noneOf(classOf[SharedAccessBlobPermissions])

      if (permission.contains("R")) {
        permissionSet.add(SharedAccessBlobPermissions.READ)
      }
      if (permission.contains("W")) {
        permissionSet.add(SharedAccessBlobPermissions.WRITE)
      }
      if (permission.contains("L")) {
        permissionSet.add(SharedAccessBlobPermissions.LIST)
      }
      if (permission.contains("D")) {
        permissionSet.add(SharedAccessBlobPermissions.DELETE)
      }
      if (permission.contains("C")) {
        permissionSet.add(SharedAccessBlobPermissions.CREATE)
      }
      if (permission.contains("A")) {
        permissionSet.add(SharedAccessBlobPermissions.ADD)
      }

      sasConstraints.setSharedAccessExpiryTime(expiryTime)
      sasConstraints.setPermissions(permissionSet)

      val blobSas = blobRef.generateSharedAccessSignature(sasConstraints, null)

      blobRef.getUri() + "?restype=blob&" + blobSas
    } else {
      throw new NotFoundException("Blob not found")
    }
  }

  def listBlobSas(container: String, permission: String, duration: Int): List[BlobInfo] = {
    val containerRef: CloudBlobContainer = blobServiceClient.getContainerReference(container)
    val sasConstraints: SharedAccessBlobPolicy = new SharedAccessBlobPolicy()
    val calendar = Calendar.getInstance()
    calendar.setTime(new Date())
    calendar.add(Calendar.HOUR, + duration)

    val expiryTime = calendar.getTime
    log.debug("Expiry time: " + expiryTime.toString)
    sasConstraints.setSharedAccessExpiryTime(expiryTime)

    val permissionSet: util.EnumSet[SharedAccessBlobPermissions] =
      util.EnumSet.noneOf(classOf[SharedAccessBlobPermissions])

    permission.foreach {
      case 'R' => permissionSet.add(SharedAccessBlobPermissions.READ)
      case 'W' => permissionSet.add(SharedAccessBlobPermissions.WRITE)
      case 'L' => permissionSet.add(SharedAccessBlobPermissions.LIST)
      case 'D' => permissionSet.add(SharedAccessBlobPermissions.DELETE)
      case 'C' => permissionSet.add(SharedAccessBlobPermissions.CREATE)
      case 'A' => permissionSet.add(SharedAccessBlobPermissions.ADD)
      case _ =>
    }

    sasConstraints.setPermissions(permissionSet)

    if(!containerRef.exists()) {
      log.info(s"The container is not exist: ${container}")
      throw new NotFoundException("Container not found")
    }

    import collection.JavaConversions._
    val blobList = containerRef.listBlobs().toList
    blobList.map(blob => {

      val uri =  blob.getStorageUri.getPrimaryUri.toString
      val fileName = uri.substring(uri.lastIndexOf('/')+1)
      val blobRef = containerRef.getBlockBlobReference(fileName)
      val blobSas = blobRef.generateSharedAccessSignature(sasConstraints, null)

      BlobInfo(blobRef.getName, blobRef.getUri() + "?restype=blob&" + blobSas)
    })
  }

  def getUploadBlobSasUri(container: String, fileName: String, duration: Int): String = {
    val containerRef: CloudBlobContainer = blobServiceClient.getContainerReference(container)

    if (!containerRef.exists()) throw new NotFoundException(s"Container not found: ${container}")

    val blobRef = containerRef.getBlockBlobReference(fileName)
    val sasConstraints: SharedAccessBlobPolicy = new SharedAccessBlobPolicy()
    val calendar = Calendar.getInstance()
    calendar.setTime(new Date())
    calendar.add(Calendar.HOUR, + duration)
    val expiryTime = calendar.getTime
    log.debug("Expiry time: " + expiryTime.toString)

    sasConstraints.setSharedAccessExpiryTime(expiryTime)
    sasConstraints.setPermissions(util.EnumSet.of(SharedAccessBlobPermissions.WRITE))

    val blobSas = blobRef.generateSharedAccessSignature(sasConstraints, null)

    return blobRef.getUri() + "?restype=blob&" + blobSas
  }

  def createContainer(containerName: String): Option[ContainerInfo] = {
    log.info("create container: " + containerName)
    val containerRef: CloudBlobContainer = blobServiceClient.getContainerReference(containerName)

    if (containerRef.exists()) throw new InvalidInputException(s"Container $containerName is exist")

    containerRef.create()

    val permission = containerRef.downloadPermissions()

    permission.setPublicAccess(BlobContainerPublicAccessType.BLOB)
    containerRef.uploadPermissions(permission)

    if (containerRef.createIfNotExists())
      Some(ContainerInfo(Some(containerName),Some(containerRef.getUri.toString)))
    else None
  }

  def listContainers(): List[ContainerInfo] = {
    import collection.JavaConverters._
    blobServiceClient.listContainers().asScala.toList.map(container => ContainerInfo(Some(container.getName), Some(container.getUri.toString)))
  }

  def deleteContainer(container: String): Boolean = {
    val containerRef: CloudBlobContainer = blobServiceClient.getContainerReference(container)
    containerRef.deleteIfExists()
  }

  def deleteBlob(container: String, blobName: String): Boolean = {
    val containerRef = blobServiceClient.getContainerReference(container)
    val blobRef = containerRef.getBlockBlobReference(blobName)

    blobRef.deleteIfExists()
  }

  def isContainerExist(containerName: String): Boolean =
    blobServiceClient.getContainerReference(containerName).exists()

  def isBlobExist(containerName: String, blobName: String): Boolean =
    blobServiceClient.getContainerReference(containerName).getBlockBlobReference(blobName).exists()

}

object AzureStorageClient {
  def apply(protocol: String, accountName: String, accountKey: String): AzureStorageClient = {
    val azureClient = new AzureStorageClient(protocol, accountName, accountKey)
    azureClient.init()

    return azureClient
  }
}