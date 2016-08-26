package service

import java.util.concurrent.atomic.AtomicReference

import model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import javax.annotation.concurrent.ThreadSafe

import utils.Const


/**
  * Created by taras.beletsky on 8/18/16.
  */
@ThreadSafe
final class ThrottlingService(ctx: Context) {

  val slaService: SlaService = new SlaServiceImpl(ctx)

  def isRequestAllowed(otoken: Option[String]): Boolean = otoken.flatMap(ctx.getCachedSlaByToken(_)).map(_.get().isRequestAllowed).getOrElse(false)

  def getSlaByToken(token: String): Option[Sla] = ctx.getCachedSlaByToken(token) match {
    case Some(userReqInfo) => getSla(userReqInfo)
    case _ =>
      //evaluate sla service and cache result
      slaService.getSlaByToken(token).onComplete {
        case Success(sla) => ctx.addCachedSla(token, sla)
        case Failure(ex) =>
        //slaService returns Future[Sla], means only way to say no-sla is to throw exception, but just skip it for now
        //throw ex
      }

      getSlaByToken("")
  }


  //evident throttle solution also throttle can be impl via DelayedQueue
  private def getSla(atomicReqInfo: AtomicReference[UserReqInfo]): Option[Sla] = atomicReqInfo.get().fsla.value match {
    case Some(Success(sla)) =>

      //non blocking atomic mutation
      while (true) {

        //deal with concrete value, we expect unchanged for next some cpu cycles
        val current = atomicReqInfo.get()

        //not waiting for quota
        if (current.busyTime == Const.NotBusy) {
          if (current.calls > 1) {
            //have available calls
            if (atomicReqInfo.compareAndSet(current, current.copy(calls = current.calls - 1))) return Some(sla)
          } else if (current.calls == 1) {
            //quote exhausted, we should start waiting for another one
            if (atomicReqInfo.compareAndSet(current, current.copy(calls = 0, busyTime = System.currentTimeMillis()))) return Some(sla)
          } //impossible case but we should handle it
          else throw new RuntimeException("if calls is 0 then busyTime should != 0")
        }
        //waiting for quota
        else {
          //may be 100ms passed by
          if (System.currentTimeMillis() - current.busyTime >= Const.QuotaTimeFrame) {
            val calls = Math.ceil(sla.maxRps.toDouble / Const.QuotesPerSecond).toInt
            if (calls > 1) {
              if (atomicReqInfo.compareAndSet(current, current.copy(calls = calls - 1, busyTime = Const.NotBusy))) return Some(sla)
            } else {
              if (atomicReqInfo.compareAndSet(current, current.copy(calls = 0, busyTime = System.currentTimeMillis()))) return Some(sla)
            }
          }
          //not yet available
          else return None
        }

      }

      None //never reached code line

    case Some(Failure(e)) =>
      throw new RuntimeException("sla service should evaluate sla w/o failures", e)
    case None => None
  }


}

