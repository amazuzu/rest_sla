package rest

import model.Sla
import org.specs2.mutable.Specification
import service.{Context, SlaRequirements}
import spray.http.{BasicHttpCredentials, StatusCodes}
import spray.httpx.SprayJsonSupport._
import spray.testkit.Specs2RouteTest

/**
  * Created by taras.beletsky on 8/21/16.
  */
class SlaThrottleRouterSpec extends Specification with Specs2RouteTest with SlaRequirements {

  sequential

  val ctx = new Context
  val slaRouter = new SlaRouter(ctx)
  val thRouter = new SlaThrottleRouter(ctx)

  def expectAllowed(allowed: Boolean) = Get("/allowed") ~> thRouter.operations ~> check(responseAs[String] == allowed.toString)

  def expectAllowedAuth(allowed: Boolean, user: String, password: String) =
    Get("/allowed") ~> addCredentials(BasicHttpCredentials(user, password)) ~> thRouter.operations ~>
      check(responseAs[String] == allowed.toString)

  "throttle service" should {

    RuleNoToken
    "return default sla" in {
      expectAllowed(true)
      Get("/sla") ~> thRouter.operations ~> check(responseAs[Sla] == Sla("", 10))
    }

    RuleUnauthGrace
    "return error within 100ms interval" in {
      expectAllowed(false)
      Get("/sla") ~> thRouter.operations ~> check(response.status == StatusCodes.TooManyRequests)
    }

    "return sla after 100ms interval" in {
      //wait till new quota
      Thread.sleep(100)
      expectAllowed(true)
      Get("/sla") ~> thRouter.operations ~> check(responseAs[Sla] == Sla("", 10))
    }

    "return error such after previous successful call" in {
      expectAllowed(false)
      Get("/sla") ~> thRouter.operations ~> check(response.status == StatusCodes.TooManyRequests)
    }

    "configure GraceRps" in {
      Post("/graceRps?rps=50") ~> thRouter.operations ~> check(response.status == StatusCodes.OK)
    }

    "configure illegal GraceRps < 10" in {
      Post("/graceRps?rps=5") ~> thRouter.operations ~> check(response.status == StatusCodes.PreconditionFailed)
    }

    RuleRpsLimit
    "support GraceRps works by calling 50/10 = 5 requests" in {
      //wait till new quota
      Thread.sleep(100)

      expectAllowed(true)
      Get("/sla") ~> thRouter.operations ~> check(responseAs[Sla] === Sla("", 50))
      expectAllowed(true)
      Get("/sla") ~> thRouter.operations ~> check(responseAs[Sla] === Sla("", 50))
      expectAllowed(true)
      Get("/sla") ~> thRouter.operations ~> check(responseAs[Sla] === Sla("", 50))
      expectAllowed(true)
      Get("/sla") ~> thRouter.operations ~> check(responseAs[Sla] === Sla("", 50))
      expectAllowed(true)
      Get("/sla") ~> thRouter.operations ~> check(responseAs[Sla] === Sla("", 50))

      //sure 6 > 5 and request should fail
      expectAllowed(false)
      Get("/sla") ~> thRouter.operations ~> check(response.status === StatusCodes.TooManyRequests)
    }

    RuleTokenNoSla
    "return sla for unknown authorized as for unauthorized user" in {

      //error
      expectAllowedAuth(false, "foo", "pwd")
      Get("/sla") ~> addCredentials(BasicHttpCredentials("foo", "pwd")) ~> thRouter.operations ~>
        check(response.status === StatusCodes.TooManyRequests)

      //wait till new quota
      Thread.sleep(100)

      //sla
      expectAllowedAuth(true, "foo", "pwd")
      Get("/sla") ~> addCredentials(BasicHttpCredentials("foo", "pwd")) ~> thRouter.operations ~>
        check(responseAs[Sla] === Sla("", 50))
    }

    NoteCache //here unauth user sla returns, since not stated explicitly in requirements
    "return unauthourized user sla after user rps added but not cached yet" in {
      Post("/graceRps?rps=10") ~> thRouter.operations ~> check(response.status == StatusCodes.OK)

      //wait till new quota
      Thread.sleep(100)

      //create user foo2
      Post("/user?name=foo2&password=pwd&rps=30") ~> slaRouter.operations ~> check(response.status == StatusCodes.OK)

      //get foo sla => should return anonym sla
      expectAllowedAuth(true, "foo2", "pwd")
      Get("/sla") ~> addCredentials(BasicHttpCredentials("foo2", "pwd")) ~> thRouter.operations ~>
        check(responseAs[Sla] === Sla("", 10))

      //after expensive sla execution completed
      Thread.sleep(300)

      //return cached value
      expectAllowedAuth(true, "foo2", "pwd")
      Get("/sla") ~> addCredentials(BasicHttpCredentials("foo2", "pwd")) ~> thRouter.operations ~>
        check(responseAs[Sla] === Sla("foo2", 30))
    }


    "handle authorized users sla requests" in {

      //eat rest of quota=3
      expectAllowedAuth(true, "foo2", "pwd")
      Get("/sla") ~> addCredentials(BasicHttpCredentials("foo2", "pwd")) ~> thRouter.operations ~> check(responseAs[Sla] === Sla("foo2", 30))
      expectAllowedAuth(true, "foo2", "pwd")
      Get("/sla") ~> addCredentials(BasicHttpCredentials("foo2", "pwd")) ~> thRouter.operations ~> check(responseAs[Sla] === Sla("foo2", 30))

      //wait till new quota
      Thread.sleep(100)

      expectAllowedAuth(true, "foo2", "pwd")
      Get("/sla") ~> addCredentials(BasicHttpCredentials("foo2", "pwd")) ~> thRouter.operations ~> check(responseAs[Sla] === Sla("foo2", 30))
      expectAllowedAuth(true, "foo2", "pwd")
      Get("/sla") ~> addCredentials(BasicHttpCredentials("foo2", "pwd")) ~> thRouter.operations ~> check(responseAs[Sla] === Sla("foo2", 30))
      expectAllowedAuth(true, "foo2", "pwd")
      Get("/sla") ~> addCredentials(BasicHttpCredentials("foo2", "pwd")) ~> thRouter.operations ~> check(responseAs[Sla] === Sla("foo2", 30))


      // 4 > 3 and request should fail
      expectAllowedAuth(false, "foo2", "pwd")
      Get("/sla") ~> addCredentials(BasicHttpCredentials("foo2", "pwd")) ~> thRouter.operations ~> check(response.status === StatusCodes.TooManyRequests)
    }

    RuleTokenSla
    "handle authorized users with another token" in {
      Thread.sleep(100)
      //eat rest of quota=3
      expectAllowedAuth(true, "foo2", "pwd2")
      Get("/sla") ~> addCredentials(BasicHttpCredentials("foo2", "pwd2")) ~> thRouter.operations ~> check(responseAs[Sla] === Sla("foo2", 30))
      expectAllowedAuth(true, "foo2", "pwd2")
      Get("/sla") ~> addCredentials(BasicHttpCredentials("foo2", "pwd2")) ~> thRouter.operations ~> check(responseAs[Sla] === Sla("foo2", 30))
    }

  }
}