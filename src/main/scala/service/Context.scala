package service

import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.ThreadSafe

import model.{Sla, UserReqInfo}
import spray.caching.{Cache, LruCache}

import scala.collection.concurrent.TrieMap

/**
  * Created by taras.beletsky on 8/20/16.
  */

//context as shared data storage among threads
@ThreadSafe
class Context {


  //todo allow threads to change map
  //map username -> sla
  val slaMap = new TrieMap[String, Sla]

  def reset() = {
    slaMap.clear()
  }

}



