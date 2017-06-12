package freedomandy.protocol

import freedomandy.data.ContainerInfo

/**
  * Created by andy on 09/06/2017.
  */
trait ContainerRequestMessage extends RequestMessage
case class CreateContainerRequest(containerName: String) extends ContainerRequestMessage
case class ListContainersRequest() extends ContainerRequestMessage
case class DeleteContainerRequest(containerName: String) extends ContainerRequestMessage

trait ContainerResultMessage extends ResultMessage
case class CreateContainerResult(containerInfo: Option[ContainerInfo]) extends ContainerResultMessage
case class ListContainersResult(containers: List[ContainerInfo]) extends ContainerResultMessage
case class DeleteContainerResult(isDeleted: Boolean) extends ContainerResultMessage