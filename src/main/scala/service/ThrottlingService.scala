package service

import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.ThreadSafe

import model._
import scaldi.{Injectable, Injector}
import spray.caching._
import utils.Const

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by taras.beletsky on 8/18/16.
  */
@ThreadSafe
final class ThrottlingService(implicit inj: Injector) extends TokenService with Const with Injectable {

  private val cache: Cache[AtomicReference[UserReqInfo]] = LruCache(timeToLive = 10 seconds)

  private val slaService: SlaService = inject[SlaService]

  //default user quota
  private val UnauthorizedUserInfo: AtomicReference[UserReqInfo] = new AtomicReference(UserReqInfo.from(Sla(EmptyUserName, MinRps)))


  private def askCache(token: String): Future[AtomicReference[UserReqInfo]] = getUsernameByToken(token).map { username =>
    //crazy spray cache! w/o get() method it doesnt return completed future!
    cache.get(username).getOrElse(cache(username) {
      slaService.getSlaByToken(token).map(sla => new AtomicReference[UserReqInfo](UserReqInfo.from(sla)))
    })

  }.getOrElse(Future.successful(UnauthorizedUserInfo))


  def updateGraceRps(rps: Int): Unit = UnauthorizedUserInfo.set(UserReqInfo.from(Sla(EmptyUserName, rps)))

  def isRequestAllowed(otoken: Option[String]): Boolean = otoken.flatMap(getReqInfoByToken(_)).map(_.get().isRequestAllowed).getOrElse(false)

  def getSlaByToken(token: String): Option[Sla] = getReqInfoByToken(token).flatMap(getSla(_))


  private def getReqInfoByToken(token: String): Option[AtomicReference[UserReqInfo]] = {
    if (token == EmptyUserToken) Some(UnauthorizedUserInfo)
    else
      askCache(token).value match {
        case Some(Success(reqInfo)) => Some(reqInfo)
        case Some(Failure(ex)) =>
          //slaService returns Future[Sla], means only way to say no-sla is to throw exception, but just skip it for now
          None
        case None => Some(UnauthorizedUserInfo)
      }
  }


  //evident throttle solution also throttle can be impl via DelayedQueue
  private def getSla(atomicReqInfo: AtomicReference[UserReqInfo]): Option[Sla] = atomicReqInfo.get().fsla.value match {
    case Some(Success(sla)) =>

      //non blocking atomic mutation
      while (true) {
        //deal with concrete value, we expect unchanged for next some cpu cycles
        val current = atomicReqInfo.get()

        //not waiting for quota
        if (current.busyTime == NotBusy) {
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
          if (System.currentTimeMillis() - current.busyTime >= QuotaTimeFrame) {
            val calls = Math.ceil(sla.maxRps.toDouble / QuotesPerSecond).toInt
            if (calls > 1) {
              if (atomicReqInfo.compareAndSet(current, current.copy(calls = calls - 1, busyTime = NotBusy))) return Some(sla)
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

  override def reset(): Unit = {
    super.reset()
    cache.clear()
    updateGraceRps(MinRps)
  }

  updateGraceRps(MinRps)
}

