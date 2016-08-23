package service

import model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import javax.annotation.concurrent.ThreadSafe


/**
  * Created by taras.beletsky on 8/18/16.
  */
@ThreadSafe
final class ThrottlingService(ctx: Context) {

  //println(s"service created ${System.currentTimeMillis()} ${Thread.currentThread().getName}")

  val slaService: SlaService = new SlaServiceImpl(ctx)

  def isRequestAllowed(otoken: Option[String]): Boolean = otoken.flatMap(ctx.getCachedSlaByToken(_))
    .getOrElse(ctx.UnauthorizedUserInfo).isRequestAllowed

  def getSlaByToken(token: String): Option[Sla] =
    if (token == "") getSla(ctx.UnauthorizedUserInfo)
    else
      ctx.getCachedSlaByToken(token) match {
        case Some(userReqInfo) => getSla(userReqInfo)
        case _ =>
          //evaluate sla service and cache result
          slaService.getSlaByToken(token).onComplete {
            case Success(sla) => ctx.addCachedSla(token, sla)
            case Failure(ex) => throw ex
          }

          getSla(ctx.UnauthorizedUserInfo)
      }


  //evident throttle solution also throttle can be impl via DelayedQueue
  private def getSla(reqInfo: UserReqInfo): Option[Sla] = reqInfo.fsla.value match {
    case Some(Success(sla)) =>
      //lock userInfo for change
      reqInfo.synchronized[Option[Sla]] {

        //not waiting for quota
        if (reqInfo.busyTime == 0L) {

          //have available calls
          if (reqInfo.calls > 0) {

            reqInfo.calls -= 1

            //quote exhausted, we should start waiting for another one
            reqInfo.checkBusy()

            Some(sla)
          }
          //impossible case but we should handle it
          else throw new RuntimeException("if calls is 0 then busyTime should != 0")

        }
        //waiting for quota
        else {

          //may be 100ms passed by
          if (System.currentTimeMillis() - reqInfo.busyTime >= 100L) {

            reqInfo.busyTime = 0L
            reqInfo.calls = Math.ceil(sla.maxRps.toDouble / 10.0).toInt - 1

            //quote exhausted, we should start waiting for another one
            reqInfo.checkBusy()

            Some(sla)
          }
          //not yet
          else None

        }
      }
    case Some(Failure(e)) =>
      throw new RuntimeException("sla service should evaluate sla w/o failures", e)
    case None => Some(ctx.DefaultSla)
  }


}

