package freedomandy.client

import org.apache.http.Header
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.{BasicResponseHandler, CloseableHttpClient, HttpClients}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.slf4j.LoggerFactory

/**
  * Created by andy on 05/06/2017.
  */
trait HttpClient {
  val logg =LoggerFactory.getLogger(this.getClass)
  val client: CloseableHttpClient = initHttpClient()
  var connMgr: PoolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager()

  def initHttpClient(): CloseableHttpClient = {
    val config = RequestConfig.custom().build()
    val client: CloseableHttpClient = HttpClients.custom()
      .setConnectionManager(connMgr)
      .setDefaultRequestConfig(config).build()
    return client
  }

  def close(): Unit = {
    connMgr.close()
  }

  def handlePostRequest(url: String, headers: List[Header], jsonBody: String): String = {
    val post = new HttpPost(url)
    headers.foreach { post.addHeader(_) }
    val entity = new ByteArrayEntity(jsonBody.getBytes("UTF-8"))
    post.setEntity(entity)
    val result = client.execute(post, new BasicResponseHandler())
    return result
  }
}




