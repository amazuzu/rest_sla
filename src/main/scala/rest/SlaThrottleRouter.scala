package rest

import javax.ws.rs.Path

import akka.actor.ActorRefFactory
import com.wordnik.swagger.annotations._
import model.Sla
import scaldi.{Injectable, Injector}
import service.ThrottlingService
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.{HttpService, Route}
import utils.Const

/**
  * Created by taras.beletsky on 8/18/16.
  */
@Api(value = "/throttle", description = "throttle sla impl")
class SlaThrottleRouter(implicit val actorRefFactory: ActorRefFactory, inj:Injector) extends HttpService with Injectable{

  val service = inject[ThrottlingService]

  val operations: Route = GetSla ~ GetAllowed ~ PostGraceRps

  @Path(value = "/sla")
  @ApiOperation(httpMethod = "GET", response = classOf[Sla], value = "return sla by token")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "token", required = false, dataType = "string", paramType = "token", value = "user basic auth token")
  ))
  @ApiResponses(Array(new ApiResponse(code = 200, message = "Ok"), new ApiResponse(code = 429, message = "TooManyRequests")))
  def GetSla = (path("sla") & get) {
    optionalHeaderValueByName("Authorization") { otoken =>
      respondWithMediaType(`application/json`) {
        service.getSlaByToken(otoken.getOrElse("")) match {
          case Some(sla) => complete(OK, sla)
          case None => complete(TooManyRequests)
        }
      }
    }
  }

  @Path(value = "/allowed")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "is allowed to call within rps", produces = "text/plain")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "token", required = false, dataType = "string", paramType = "query", value = "user basic auth token")
  ))
  def GetAllowed = (path("allowed") & get) {
    optionalHeaderValueByName("Authorization") { otoken =>
      complete(OK, "" + service.isRequestAllowed(otoken))
    }
  }

  @Path(value = "/graceRps")
  @ApiOperation(httpMethod = "POST", value = "configure GraceRps")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "rps", required = true, dataType = "string", paramType = "query", value = "max request per second")
  ))
  @ApiResponses(Array(new ApiResponse(code = 200, message = "Ok"), new ApiResponse(code = 412, message = "PreconditionFailed")))
  def PostGraceRps = (path("graceRps") & post) {
    parameters('rps.as[Int]) { rps =>

      if (rps >= Const.MinRps) {
        service.updateGraceRps(rps)
        complete(OK)
      } else
        complete(PreconditionFailed)

    }
  }


}
