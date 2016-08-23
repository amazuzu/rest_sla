package model

import scala.concurrent.Future

/**
  * Created by taras.beletsky on 8/22/16.
  */
class UserReqInfo(val fsla: Future[Sla], var calls: Int, var busyTime: Long) {
  def checkBusy() = if (calls == 0) busyTime = System.currentTimeMillis()

  def isRequestAllowed = busyTime == 0L && calls > 0
}

object UserReqInfo {
  def apply(sla: Sla) = new UserReqInfo(Future.successful(sla), Math.ceil(sla.maxRps.toDouble / 10.0).toInt, 0L)
}