package service

import utils.Utils

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

/**
  * Created by taras on 8/27/16.
  */
trait TokenService {

  //map token -> username
  protected val tokenMap = new TrieMap[String, String]

  //get value by token, if fail then by extracted username
  protected def getByToken[T](token: String, mapGet: String => Option[T]): Option[T] = tokenMap.get(token) match {
    case Some(username) => mapGet(username)
    case _ => Utils.decodeBase64BasicUsername(token) match {
      case Some(username) => mapGet(username)
      case _ => None
    }
  }

  protected def getUsernameByToken(token: String): Option[String] = tokenMap.get(token) match {
    case su@Some(username) => su
    case _ => Utils.decodeBase64BasicUsername(token) match {
      case su@Some(username) => su
      case _ => None
    }
  }

  def reset(): Unit = tokenMap.clear()
}

