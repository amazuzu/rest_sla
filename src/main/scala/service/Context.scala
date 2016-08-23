package service

import javax.annotation.concurrent.ThreadSafe

import model.{Sla, UserReqInfo}
import utils.Utils

import scala.collection.concurrent.TrieMap

/**
  * Created by taras.beletsky on 8/20/16.
  */
@ThreadSafe
class Context {

  //configurable
  private var GraceRps: Int = 10

  //default user quota, only getter is visible
  private var _UnauthorizedUserInfo: UserReqInfo = null

  def UnauthorizedUserInfo = _UnauthorizedUserInfo

  //unwrapped default sla value
  private var _DefaultSla: Sla = null

  def DefaultSla = _DefaultSla


  //map token -> username
  private val tokenMap = new TrieMap[String, String]

  //map username -> sla
  private val slaMap = new TrieMap[String, Sla]

  //map username -> future[sla]
  private val cacheMap = new TrieMap[String, UserReqInfo]

  def addSla(token: String, sla: Sla): Unit = {
    tokenMap += token -> sla.userName
    slaMap += sla.userName -> sla
  }

  def addCachedSla(token: String, sla: Sla): Unit = {
    tokenMap += token -> sla.userName
    cacheMap += sla.userName -> UserReqInfo(sla)
  }

  def getSlaByToken(token: String) = getByToken(token, slaMap.get)

  def getCachedSlaByToken(token: String) = getByToken(token, cacheMap.get)

  //get value by token, if fail then by extracted username
  private def getByToken[T](token: String, mapGet: String => Option[T]): Option[T] =
    if (token == "") None
    else tokenMap.get(token) match {
      case Some(username) => mapGet(username)
      case _ => Utils.decodeBase64BasicUsername(token) match {
        case Some(username) => mapGet(username)
        case _ => None
      }
    }

  def updateGraceRps(rps: Int) = synchronized {
    GraceRps = rps
    _DefaultSla = Sla("", rps)
    _UnauthorizedUserInfo = UserReqInfo(_DefaultSla)
    cacheMap += "" -> UnauthorizedUserInfo
  }

  def reset() = synchronized {
    tokenMap.clear()
    slaMap.clear()
    cacheMap.clear()
    updateGraceRps(10)
  }

  updateGraceRps(GraceRps)
}



