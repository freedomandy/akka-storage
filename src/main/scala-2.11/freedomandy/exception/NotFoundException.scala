package freedomandy.exception

/**
  * Created by andy on 05/06/2017.
  */
class NotFoundException(errorMsg: String) extends BaseException(errorMsg) {
  override def errorCode = "Resource Not Found"
  override def httpStatusCode = 404
}
