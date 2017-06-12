package freedomandy.exception

/**
  * Created by andy on 05/06/2017.
  */
case class BaseException(errorMsg: String) extends Exception(errorMsg) {
  def errorCode: String = "Interval Error"
  def httpStatusCode: Int = 500
}
