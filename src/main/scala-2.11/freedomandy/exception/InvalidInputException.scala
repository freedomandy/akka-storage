package freedomandy.exception

/**
  * Created by andy on 05/06/2017.
  */
class InvalidInputException(override val errorMsg: String) extends BaseException(errorMsg) {
  override def errorCode = "Invalid Input"
  override def httpStatusCode = 400
}
