package model

import utils.Const

import scala.concurrent.Future

/**
  * Created by taras.beletsky on 8/22/16.
  */
case class UserReqInfo(fsla: Future[Sla], calls: Int, busyTime: Long) {
  def isRequestAllowed = busyTime == Const.NotBusy && calls > 0
}

object UserReqInfo {
  def from(sla: Sla) = UserReqInfo(Future.successful(sla), Math.ceil(sla.maxRps.toDouble / Const.QuotesPerSecond).toInt, Const.NotBusy)
}