import akka.actor.ActorSystem
import rest.{ApiRouterActor, SlaRouter, SlaThrottleRouter}
import scaldi.Module
import service.{Context, SlaService, SlaServiceImpl, ThrottlingService}
import utils.Config._

/**
  * Created by taras on 8/27/16.
  */
class BootModule extends Module {

  val system = ActorSystem(app.systemName)

  bind[ActorSystem] to system destroyWith (_.terminate())

  bind[Context] to injected[Context]

  bind[SlaService] to injected[SlaServiceImpl]
  bind[ThrottlingService] to injected[ThrottlingService]

  bind[SlaRouter] to injected[SlaRouter]
  bind[SlaThrottleRouter] to injected[SlaThrottleRouter]

  binding toProvider new ApiRouterActor
}
