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

  (LoggerFactory.getILoggerFactory()).asInstanceOf[LoggerContext].stop()


  val conf = ConfigFactory.load()

  val httpConf = http.baseURL("http://localhost:8080")

  val feeder = Iterator.continually(Map("user" -> Random.nextInt(1000000).toString, "pwd" -> Random.nextInt(100).toString))

  val reset = scenario("reset").exec(http("reset ctx").get("/reset"))

  val getThSla = scenario("get throttle sla scenario")
    .feed(feeder)
    .exec(http("add user ${user}")
      .post("/user").queryParam("name", "${user}").queryParam("password", "${pwd}").queryParam("rps", Random.nextInt(1000) + 200))
    .pause(5 millis)
    .repeat(3, "n") {
      exec(http("get sla ${user} ${n}")
        .get("/throttle/sla").basicAuth("${user}", "${pwd}")
      ) //.pause(Random.nextInt(100) + 30 millis)
    }

  val getSla = scenario("get sla scenario")
    .pause(3 seconds)
    .feed(feeder)
    .exec(http("add user ${user}")
      .post("/user").queryParam("name", "${user}").queryParam("password", "${pwd}").queryParam("rps", Random.nextInt(1000) + 200))
    .pause(5 millis)
    .repeat(10, "n") {
      exec(http("get sla ${user} ${n}")
        .get("/sla")
      ) //.pause(Random.nextInt(100) + 30 millis)
    }


  setUp(
    reset.inject(atOnceUsers(1)),

    //getThSla.inject(constantUsersPerSec(40) during (10 seconds))
    getSla.inject(constantUsersPerSec(40) during (10 seconds))

  ).protocols(httpConf)

  /*


      //graceRps.inject(atOnceUsers(1))
    //getUndefinedSla.inject(atOnceUsers(1))
    //getUndefinedSla.exec(getDefinedSla).inject(atOnceUsers(1))
    //getUndefinedSla.inject(atOnceUsers(100))
    //registerFoo.inject(rampUsers(100) over (1 minute))
    //registerFoo.inject(atOnceUsers(1))

    //getThSla.inject(rampUsers(10000) over (15 seconds))
    //getSla.inject(rampUsers(100) over (15 seconds))

  val graceRps = scenario("work with GraceRps")
    .exec(http("ask default gracerps").get("/throttle/sla")
      .check(jsonPath("$.maxRps").is(mrps => "1")))
    .exec(http("update grace rps").post("/throttle/graceRps").queryParam("rps", 10))
    .exec(http("get grace rps").get("/throttle/sla")
      .check(jsonPath("$.maxRps").is(mrps => "10")))

  val getUndefinedSla = scenario("get undefined sla")
    .exec(http("get sla 0").get("/throttle/sla").basicAuth("foo", "bar"))
    .pause(10 millis)
    .exec(http("get sla").get("/throttle/sla").basicAuth("foo2", "bar")
      .check(status.is(404)))

  val getDefinedSla = scenario("get undefined sla")
    .exec(http("get sla 0").get("/throttle/sla").basicAuth("foo", "bar"))
    .pause(110 millis)
    .exec(http("get sla").get("/throttle/sla").basicAuth("foo2", "bar")
      .check(status.is(200)))


  val registerFoo = scenario("register users").repeat(10, "n") {
    exec(http("add user ${n}")
      .post("/user").queryParam("name", "user${n}").queryParam("password", "pwd").queryParam("rps", Random.nextInt(2000) + 20))
  }

  val unauthGetSla = scenario("unauth sla request").repeat(10, "n") {
    exec(http("get sla")
      .post("/throttle/sla"))
  }*/
}