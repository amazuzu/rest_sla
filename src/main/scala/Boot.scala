import akka.actor.{ActorRef, ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import rest.ApiRouterActor
import service.Context
import spray.can.Http
import utils.Config._

import scala.concurrent.duration._


object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem(app.systemName)

  // parallel execution guarantee
  val userActor: ActorRef = system.actorOf(Props(classOf[ApiRouterActor], new Context)
    .withDispatcher("sla-dispatcher")
    .withRouter(RoundRobinPool(nrOfInstances = 1))
    , app.userServiceName)

  implicit val timeout = Timeout(5.seconds)

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(userActor, interface = app.interface, port = app.port)

}
