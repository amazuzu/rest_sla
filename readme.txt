COMPILE
sbt clean compile

RUN
sbt "run-main Boot"

TEST
sbt test

SWAGGER
run http://localhost:8080

GATLING
load test results: compare result-sla.txt to result-throttle-sla.txt

to run Gatling manually
    download https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/2.2.2/gatling-charts-highcharts-bundle-2.2.2-bundle.zip
    unzip to ~/apps/gatling
    edit script and run it sh ./src/test/scala/loadtest/run-simul0.sh


