/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val AkkaVersion = "2.6.21"
  val hmrcMongo = "1.4.0"
  val bootstrapBackend = "7.23.0"

  val compile = Seq(
    ws,
    "com.typesafe.akka" %% "akka-actor"                        % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-typed"                  % AkkaVersion,
    "com.typesafe.akka" %% "akka-protobuf-v3"                  % AkkaVersion,
    "com.typesafe.akka" %% "akka-stream"                       % AkkaVersion,
    "com.typesafe.akka" %% "akka-serialization-jackson"        % AkkaVersion,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28"         % bootstrapBackend,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-work-item-repo-play-28" % hmrcMongo,
    "uk.gov.hmrc"       %% "domain"                            % "8.3.0-play-28",
    "net.codingwell"    %% "scala-guice"                       % "5.1.1",
    "com.typesafe.play" %% "play-json-joda"                    % "2.9.4"
  )

  val test = Set(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapBackend    % "test, it",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongo           % "test,it",
    "com.github.tomakehurst" % "wiremock-standalone"      % "2.27.2"            % "test,it",
    "org.mockito"            % "mockito-core"             % "5.2.0"             % "test,it",
    "org.scalatestplus"      %% "mockito-3-4"             % "3.2.10.0"          % "test,it",
    "org.scalatest"          %% "scalatest"               % "3.2.15"            % "test,it",
    "org.pegdown"            % "pegdown"                  % "1.6.0"             % "test,it",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0"             % "test,it",
    "com.typesafe.play"      %% "play-test"               % PlayVersion.current % "test,it",
    "com.vladsch.flexmark"   % "flexmark-all"             % "0.62.2"            % "test,it",
    "com.typesafe.akka"      %% "akka-testkit"            % AkkaVersion         % Test
  )

  val overrides = Set()

}
