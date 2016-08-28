package service

import javax.annotation.concurrent.ThreadSafe

import model.Sla

import scala.collection.concurrent.TrieMap

/**
  * Created by taras.beletsky on 8/20/16.
  */

//context as shared data storage among threads
@ThreadSafe
class Context extends IReset {

  //todo allow threads to change map
  //map username -> sla
  val slaMap = new TrieMap[String, Sla]

  def reset() = {
    slaMap.clear()
  }

}

trait IReset {
  def reset(): Unit
}

