package rest

import akka.actor.{Actor, ActorLogging}
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.model.ApiInfo
import scaldi.{Injectable, Injector}
import service.Context
import spray.http.StatusCodes
import spray.routing.RejectionHandler.Default
import spray.routing._

import scala.reflect.runtime.universe._

/**
  * Created by taras.beletsky on 8/18/16.
  */
class ApiRouterActor(implicit val inj:Injector) extends Actor with HttpService with ActorLogging with Injectable{

  implicit def actorRefFactory = context

  val ctx = inject[Context]
  val slaRouter = inject[SlaRouter]
  val throttleSlaRouter = inject[SlaThrottleRouter]

  val swaggerService = new SwaggerHttpService {
    override def apiTypes = Seq(typeOf[SlaRouter], typeOf[SlaThrottleRouter])
    override def apiVersion = "2.0"
    override def baseUrl = "/"
    override def docsPath = "api-docs"
    override def actorRefFactory = context
    override def apiInfo = Some(new ApiInfo("sla throttling service", "impl of sla service and its throttle version", "", "", "", ""))
  }

  def receive = runRoute(slaRouter.operations ~ pathPrefix("throttle")(throttleSlaRouter.operations) ~
    (path("reset") & get) { req =>
      slaRouter.service.reset()
      throttleSlaRouter.service.reset()
      ctx.reset()

      complete(StatusCodes.OK)
    } ~
    swaggerService.routes ~
    get {
      pathPrefix("") { pathEndOrSingleSlash {
        getFromResource("swagger-ui/index.html")
      }
      } ~
        pathPrefix("webjars") {
          getFromResourceDirectory("META-INF/resources/webjars")
        }
    })


}