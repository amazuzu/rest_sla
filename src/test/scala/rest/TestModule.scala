package rest

import akka.actor.ActorSystem
import scaldi.Module
import service.{Context, SlaService, SlaServiceImpl, ThrottlingService}
import utils.Config.app

/**
  * Created by taras on 8/27/16.
  */
class TestModule extends Module {
  bind[ActorSystem] to ActorSystem(app.systemName) destroyWith (_.terminate())

  bind[Context] to injected[Context]

  bind[SlaService] to injected[SlaServiceImpl]
  bind[ThrottlingService] to injected[ThrottlingService]

  bind[SlaRouter] to injected[SlaRouter]
  bind[SlaThrottleRouter] to injected[SlaThrottleRouter]
}
