package freedomandy.client

import freedomandy.exception.BaseException
import org.apache.http.client.methods.{CloseableHttpResponse, HttpPut}
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.util.EntityUtils
import org.slf4j.{Logger, LoggerFactory}

/**
  * Created by andy on 05/06/2017.
  */
object AzureStorageRestClient extends HttpClient {
  val log: Logger = LoggerFactory.getLogger(this.getClass)
  def putBlock(destination: String, chunk: Array[Byte]): Unit = {
    val put:HttpPut = new HttpPut(destination)
    put.setHeader("Content-type", "application/octet-stream")

    val entity = new ByteArrayEntity(chunk)
    put.setEntity(entity)

    try {
      val response:CloseableHttpResponse = client.execute(put)
      log.debug("Response: " + response.toString)
      val statusCode: Int = response.getStatusLine.getStatusCode
      val entity = response.getEntity
      val result = if (entity == null)  "" else EntityUtils.toString(entity)
      response.close

      if (statusCode != 201) {
        log.info("result: " + result)
        throw BaseException("Failed to put block")
      }
    } catch {
      case e: Throwable =>
        log.info(e.getMessage)
        throw e
    }
  }
}
