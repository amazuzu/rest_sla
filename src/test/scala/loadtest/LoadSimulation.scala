package loadtest

import ch.qos.logback.classic.LoggerContext
import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.Random


/**
  * Created by taras.beletsky on 8/19/16.
  */
class LoadSimulation extends Simulation {

  val NumUsers = 10
  val Duration = 10
  val Rps = 500
  val Requests = 1000

  (LoggerFactory.getILoggerFactory()).asInstanceOf[LoggerContext].stop()


  val conf = ConfigFactory.load()

  val httpConf = http.baseURL("http://localhost:8080")

  val feeder = Iterator.continually(Map("user" -> Random.nextInt(1000000).toString, "pwd" -> Random.nextInt(100).toString))

  val reset = scenario("reset").exec(http("reset ctx").get("/reset").silent)


  def run(throttle: Boolean) = scenario("get throttle sla scenario")
    .feed(feeder)
    .exec(http("add user ${user}")
      .post("/user").queryParam("name", "${user}").queryParam("password", "${pwd}").queryParam("rps", Rps).silent)
    .repeat(Requests, "n") {
      exec(http("get sla ${user} ${n}")
        .get(if (throttle) "/throttle/sla" else "/sla").basicAuth("${user}", "${pwd}").check(status.is(200))
      )
    }


  setUp(
    reset.inject(atOnceUsers(1)),

    //getSla.inject(rampUsers(NumUsers) over (Duration seconds))

    run(false).inject(constantUsersPerSec(NumUsers) during (Duration seconds))
    //getSla.inject(constantUsersPerSec(NumUsers) during (Duration seconds))

  ).protocols(httpConf).maxDuration(Duration seconds)
}