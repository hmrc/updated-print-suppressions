import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings._

val appName = "updated-print-suppressions"

Global / majorVersion := 4
Global / scalaVersion := "3.3.6"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(scalaSettings *)
  .settings(defaultSettings() *)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    ConfigKey.configurationToKey(Test) / parallelExecution := false,
    routesGenerator := InjectedRoutesGenerator,
    Test / fork := false,
    retrieveManaged := true,
    scalacOptions ++= List("-feature")
  )
  .settings(RoutesKeys.routesImport ++= Seq("uk.gov.hmrc.ups.model._"))
  .settings(ScoverageSettings())

lazy val it = project
  .enablePlugins(PlayScala, ScalafmtPlugin)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(libraryDependencies ++= AppDependencies.it)

Test / test := (Test / test)
  .dependsOn(scalafmtCheckAll)
  .value

// NOTE: the jenkins build does not currently execute the integration tests 

it / test := (it / Test / test)
  .dependsOn(scalafmtCheckAll, it/scalafmtCheckAll)
  .value
