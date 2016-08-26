package service

import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.ThreadSafe

import model.{Sla, UserReqInfo}
import utils.{Const, Utils}

import scala.collection.concurrent.TrieMap

/**
  * Created by taras.beletsky on 8/20/16.
  */
@ThreadSafe
class Context {

  //configurable
  //seems unusable field
  private var GraceRps: Int = Const.MinRps

  //default user quota, only getter is visible
  //sure seems obvious to add to cache as "" -> value, but lets track it explicitly
  private var _UnauthorizedUserInfo: AtomicReference[UserReqInfo] = null

  //map token -> username
  private val tokenMap = new TrieMap[String, String]

  //map username -> sla
  private val slaMap = new TrieMap[String, Sla]

  //map username -> future[sla]
  private val cacheMap = new TrieMap[String, AtomicReference[UserReqInfo]]

  def addSla(token: String, sla: Sla): Unit = {
    tokenMap += token -> sla.userName
    slaMap += sla.userName -> sla
  }

  def addCachedSla(token: String, sla: Sla): Unit = {
    tokenMap += token -> sla.userName
    cacheMap += sla.userName -> new AtomicReference(UserReqInfo.from(sla))
  }

  def getSlaByToken(token: String) = getByToken(token, slaMap.get)

  def getCachedSlaByToken(token: String) = if (token == "") Some(_UnauthorizedUserInfo) else getByToken(token, cacheMap.get)

  //get value by token, if fail then by extracted username
  private def getByToken[T](token: String, mapGet: String => Option[T]): Option[T] = tokenMap.get(token) match {
    case Some(username) => mapGet(username)
    case _ => Utils.decodeBase64BasicUsername(token) match {
      case Some(username) => mapGet(username)
      case _ => None
    }
  }

  def updateGraceRps(rps: Int) = {
    _UnauthorizedUserInfo = new AtomicReference(UserReqInfo.from(Sla("", rps)))
    cacheMap += "" -> _UnauthorizedUserInfo
  }

  def reset() = synchronized {
    tokenMap.clear()
    slaMap.clear()
    cacheMap.clear()
    updateGraceRps(Const.MinRps)
  }

  updateGraceRps(Const.MinRps)
}



