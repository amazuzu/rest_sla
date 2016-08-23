version       := "0.1"

scalaVersion  := "2.11.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaVersion = "2.3.6"
  val sprayVersion = "1.3.2"
  Seq(
    "io.spray"            %%  "spray-can"      % sprayVersion,
    "io.spray"            %%  "spray-routing"  % sprayVersion,
    "io.spray"            %%  "spray-json"     % sprayVersion,
    "io.spray"            %%  "spray-testkit"  % sprayVersion  % "test",
    "org.specs2"          %%  "specs2-core"    % "2.3.11"      % "test",
    "com.typesafe.akka"   %%  "akka-actor"     % akkaVersion,
    "com.typesafe.akka"   %%  "akka-testkit"   % akkaVersion   % "test",
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "org.slf4j" % "slf4j-nop" % "1.6.4",
    "com.typesafe"         %  "config"         % "1.2.1",
    "com.gettyimages"     %%  "spray-swagger"  % "0.5.0",
    "org.webjars"          %  "swagger-ui"     % "2.0.12",
    "com.github.t3hnar"   %%  "scala-bcrypt"   % "2.4",
    "org.mindrot"          %  "jbcrypt"        % "0.3m",
    "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.2"
  )
}

fork in Test := false

parallelExecution in Test := false

Revolver.settings

resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo)

