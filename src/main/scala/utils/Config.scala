package utils

import com.typesafe.config.ConfigFactory


object Config {
  private val config = ConfigFactory.load()

  object app {
    val appConf = config.getConfig("app")

    val systemName = appConf.getString("systemName")
    val interface = appConf.getString("interface")
    val port = appConf.getInt("port")
    val userServiceName = appConf.getString("userServiceName")

  }

}

trait Const {

  //how often to evaluate quotes
  val QuotesPerSecond = 10.0

  //quota time frame, milliseconds
  val QuotaTimeFrame = 100L

  //minimum of allowed rps
  val MinRps = 10

  //means not waiting for new quota
  val NotBusy = 0L

  val EmptyUserName = ""
  
  val EmptyUserToken = ""
}

object Const extends Const {

}