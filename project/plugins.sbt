resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

addSbtPlugin("uk.gov.hmrc"               % "sbt-auto-build"         % "3.15.0")
addSbtPlugin("uk.gov.hmrc"               % "sbt-distributables"     % "2.2.0")
addSbtPlugin("com.typesafe.play"         % "sbt-plugin"             % "2.8.20")
addSbtPlugin("org.scoverage"             % "sbt-scoverage"          % "1.9.3")
addSbtPlugin("org.scalastyle"            %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("com.lucidchart"            % "sbt-scalafmt"           % "1.16")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"           % "0.4.2")
addDependencyTreePlugin
