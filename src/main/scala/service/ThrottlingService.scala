package service

import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.ThreadSafe

import model._
import spray.caching.{Cache, LruCache}
import utils.{Const, Utils}

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.duration._


/**
  * Created by taras.beletsky on 8/18/16.
  */
@ThreadSafe
final class ThrottlingService(ctx: Context) extends TokenService with Const {

  //private val cache: Cache[AtomicReference[UserReqInfo]] = LruCache(timeToLive = 10 seconds)

  //todo allow threads to change map
  //map username -> future[sla]
  private val cacheMap = new TrieMap[String, AtomicReference[UserReqInfo]]

  private val slaService: SlaService = new SlaServiceImpl(ctx)

  //default user quota
  private val UnauthorizedUserInfo: AtomicReference[UserReqInfo] = new AtomicReference(UserReqInfo.from(Sla(EmptyUserName, MinRps)))


  /*private def cachedOp(token: String): Future[Sla] = cache(token, () =>
    slaService.getSlaByToken(token)
  )*/


  def getCachedSlaByToken(token: String) = if (token == "") Some(UnauthorizedUserInfo) else getByToken(token, cacheMap.get)


  def updateGraceRps(rps: Int): Unit = UnauthorizedUserInfo.set(UserReqInfo.from(Sla(EmptyUserName, rps)))


  def isRequestAllowed(otoken: Option[String]): Boolean = otoken.flatMap(getCachedSlaByToken(_)).map(_.get().isRequestAllowed).getOrElse(false)

  private def addCachedSla(token: String, sla: Sla): Unit = {
    tokenMap += token -> sla.userName
    cacheMap += sla.userName -> new AtomicReference(UserReqInfo.from(sla))
  }

  def getSlaByToken(token: String): Option[Sla] = getCachedSlaByToken(token) match {
    case Some(userReqInfo) => getSla(userReqInfo)
    case _ =>
      //evaluate sla service and cache result
      slaService.getSlaByToken(token).onComplete {
        case Success(sla) => addCachedSla(token, sla)
        case Failure(ex) =>
        //slaService returns Future[Sla], means only way to say no-sla is to throw exception, but just skip it for now
        //throw ex
      }

      getSlaByToken(EmptyUserToken)
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


  updateGraceRps(MinRps)

  override def reset(): Unit = {
    super.reset()
    cacheMap.clear()
    updateGraceRps(MinRps)
  }
}

