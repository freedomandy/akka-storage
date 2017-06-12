package freedomandy.config

import com.typesafe.config.ConfigFactory

/**
  * Created by andy on 05/06/2017.
  */
object StorageModule {
  val conf = ConfigFactory.load("storage-env.conf")
  val defaultConfiguration = DefaultConfiguration(conf.getString("AzureblobService.storage.name"),
    conf.getString("AzureblobService.storage.key"))

  def getConfig: DefaultConfiguration = defaultConfiguration
}
