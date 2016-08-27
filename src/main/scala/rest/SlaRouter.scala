package rest

import javax.ws.rs.Path

import akka.actor.ActorRefFactory
import com.wordnik.swagger.annotations._
import model.Sla
import service.{Context, SlaServiceImpl}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.{HttpService, Route}
import utils.Const

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Created by taras.beletsky on 8/18/16.
  */

@Api(value = "", description = "default sla impl")
class SlaRouter(ctx: Context)(implicit val actorRefFactory: ActorRefFactory) extends HttpService {

  val service = new SlaServiceImpl(ctx)

  val operations: Route = GetSla ~ PostUser

  @Path(value = "/sla")
  @ApiOperation(httpMethod = "GET", response = classOf[Sla], value = "return sla by token")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "token", required = false, dataType = "string", paramType = "token", value = "user basic auth token")
  ))
  @ApiResponses(Array(new ApiResponse(code = 200, message = "Ok"), new ApiResponse(code = 404, message = "NotFounds")))
  def GetSla: Route = (path("sla") & get) {
    optionalHeaderValueByName("Authorization") { otoken =>
      onComplete(service.getSlaByToken(otoken.getOrElse(Const.EmptyUserToken))) {
        case Success(sla) => complete(OK, sla)
        case Failure(ex) => complete(NotFound)
      }
    }
  }

  @Path(value = "/user")
  @ApiOperation(httpMethod = "POST", response = classOf[Sla], value = "add sla for user")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "name", required = true, dataType = "string", paramType = "query", value = "user name"),
    new ApiImplicitParam(name = "password", required = true, dataType = "string", paramType = "query", value = "user password"),
    new ApiImplicitParam(name = "rps", required = true, dataType = "int", paramType = "query", value = "max request per second")

  ))
  @ApiResponses(Array(new ApiResponse(code = 200, message = "Ok")))
  def PostUser: Route = (path("user") & post) {
    parameters('name.as[String], 'password.as[String], 'rps.as[Int]) { (name, password, rps) =>

      if (rps < Const.MinRps || name == "")
        complete(PreconditionFailed)
      else {
        service.defineSla(name, password, rps)
        complete(OK)
      }
    }
  }

}
