package rest

import akka.actor.{Actor, ActorLogging}
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.model.ApiInfo
import service.Context
import spray.http.StatusCodes
import spray.routing.RejectionHandler.Default
import spray.routing._

import scala.reflect.runtime.universe._

/**
  * Created by taras.beletsky on 8/18/16.
  */
class ApiRouterActor(ctx: Context) extends Actor with HttpService with ActorLogging {

  implicit def actorRefFactory = context

  val slaRouter = new SlaRouter(ctx)

  val throttleSlaRouter = new SlaThrottleRouter(ctx)

  val swaggerService = new SwaggerHttpService {
    override def apiTypes = Seq(typeOf[SlaRouter], typeOf[SlaThrottleRouter])
    override def apiVersion = "2.0"
    override def baseUrl = "/"
    override def docsPath = "api-docs"
    override def actorRefFactory = context
    override def apiInfo = Some(new ApiInfo("sla throttling service", "impl of sla service and its throttle version", "", "", "", ""))
  }

  def receive = runRoute(slaRouter.operations ~ pathPrefix("throttle")(throttleSlaRouter.operations) ~ swaggerService.routes ~
    (path("reset") & get) { req =>
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