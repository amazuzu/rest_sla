package model

import spray.json.DefaultJsonProtocol

/**
  * Created by taras.beletsky on 8/18/16.
  */
case class Sla(userName: String, maxRps: Int)

object Sla extends DefaultJsonProtocol {
  implicit val slaFormat = jsonFormat2(Sla.apply)
}