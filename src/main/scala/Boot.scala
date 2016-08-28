import akka.actor.{ActorRef, ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import rest.ApiRouterActor
import scaldi.akka.AkkaInjectable
import spray.can.Http
import utils.Config._

import scala.concurrent.duration._


object Boot extends App with AkkaInjectable {

  implicit val appModule = new BootModule

  implicit val system = inject[ActorSystem]

  // parallel execution guarantee
  val userActor: ActorRef = system.actorOf(injectActorProps[ApiRouterActor].withDispatcher("sla-dispatcher")
    .withRouter(RoundRobinPool(nrOfInstances = 1)))

  implicit val timeout = Timeout(5.seconds)

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(userActor, interface = app.interface, port = app.port)

}
