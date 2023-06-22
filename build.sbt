import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings._

val appName = "updated-print-suppressions"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(majorVersion := 3)
  .settings(scalaSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    scalaVersion := "2.13.8",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    ConfigKey.configurationToKey(Test) / parallelExecution := false,
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
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / parallelExecution := false,
    routesGenerator := InjectedRoutesGenerator,
  )
  .settings(ScoverageSettings())
