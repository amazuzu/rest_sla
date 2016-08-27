package service

import javax.annotation.concurrent.ThreadSafe

import model.Sla
import utils.Utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
  * Created by taras.beletsky on 8/18/16.
  */
trait SlaService {
  def getSlaByToken(token: String): Future[Sla]

  def defineSla(username: String, password: String, rps: Int): Unit
}

@ThreadSafe
final class SlaServiceImpl(val ctx: Context) extends SlaService with TokenService {

  def defineSla(username: String, password: String, rps: Int): Unit = {
    tokenMap += Utils.encodeBasicToken(username, password) -> username
    ctx.slaMap += username -> Sla(username, rps)
  }


  private def expensiveEval(sla: Sla) = Future {
    //suppose magic number is allowed here
    Thread.sleep(20)
    sla
  }

  //expensive method
  override def getSlaByToken(token: String) = getByToken(token, ctx.slaMap.get) match {
    case Some(sla) => expensiveEval(sla)
    //must throw exception since no-sla
    case _ => Future.failed(new IllegalArgumentException(s"no sla found for token ${token}"))
  }

  override def reset(): Unit = {
    super.reset()
  }
}