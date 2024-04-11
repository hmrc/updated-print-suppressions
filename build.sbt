import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings._

val appName = "updated-print-suppressions"

Global / majorVersion := 3
Global / scalaVersion := "2.13.12"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(scalaSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    ConfigKey.configurationToKey(Test) / parallelExecution := false,
    routesGenerator := InjectedRoutesGenerator,
    Test / fork := false,
    retrieveManaged := true,
    scalacOptions ++= List(
      "-feature", "-Xlint",
      // Silence unused warnings on Play `routes` files
      "-Wconf:cat=unused-imports&src=.*routes.*:s",
      "-Wconf:cat=unused-privates&src=.*routes.*:s"
    )
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
  .dependsOn(itdisabled, scalafmtCheckAll, it/scalafmtCheckAll)
  .value
