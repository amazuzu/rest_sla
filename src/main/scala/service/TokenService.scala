package service

import utils.{Const, Utils}

import scala.collection.concurrent.TrieMap

/**
  * Created by taras on 8/27/16.
  */
trait TokenService extends IReset {

  //map token -> username
  private val tokenMap = new TrieMap[String, String]

  protected def getUsernameByToken(token: String): Option[String] =
    if (token == Const.EmptyUserToken) throw new RuntimeException("empty token should be handled explicitly")
    else
      tokenMap.get(token) match {
        case su@Some(username) => su
        case _ => Utils.decodeBase64BasicUsername(token) match {
          case su@Some(username) =>
            //wont decode token next time
            addToken(token, username)
            su
          case _ => None
        }
      }

  def addToken(token: String, username: String): Unit = tokenMap += token -> username

  def reset(): Unit = tokenMap.clear()
}

