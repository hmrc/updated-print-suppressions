/*
 * Copyright 2020 HM Revenue & Customs
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

  val AkkaVersion = "2.6.20"

  val compile = Seq(
    ws,
    "com.typesafe.akka"         %% "akka-actor"                        % AkkaVersion,
    "com.typesafe.akka"         %% "akka-actor-typed"                  % AkkaVersion,
    "com.typesafe.akka"         %% "akka-protobuf-v3"                  % AkkaVersion,
    "com.typesafe.akka"         %% "akka-stream"                       % AkkaVersion,
    "com.typesafe.akka"         %% "akka-serialization-jackson"        % AkkaVersion,
    "uk.gov.hmrc"               %% "bootstrap-backend-play-28"         % "6.4.0",
    "uk.gov.hmrc"               %% "auth-client"                       % "5.7.0-play-28",
    "uk.gov.hmrc"               %% "time"                              % "3.3.0",
    "uk.gov.hmrc.mongo"         %% "hmrc-mongo-play-28"                % "0.70.0",
    "uk.gov.hmrc.mongo"         %% "hmrc-mongo-work-item-repo-play-28" % "0.70.0",
    "uk.gov.hmrc"               %% "play-scheduling-play-28"           % "8.1.0",
    "uk.gov.hmrc"               %% "domain"                            % "6.2.0-play-28",
    "net.codingwell"            %% "scala-guice"                       % "5.1.0",
    "com.typesafe.play"         %% "play-json-joda"                    % "2.9.2",
    "com.typesafe.play"         %% "play-iteratees"                    % "2.6.1",
    "com.vladsch.flexmark"         % "flexmark-all"                    % "0.64.0"
  )

  val test = Set(
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-test-play-28"  % "0.70.0"            % "test,it",
    "com.github.tomakehurst"       % "wiremock-jre8"             % "2.31.0"            % "test,it",
    "org.mockito"                  % "mockito-core"              % "4.8.0"             % "test,it",
    "org.scalatestplus"            %% "mockito-3-4"              % "3.2.10.0"          % "test,it",
    "org.scalatest"                %% "scalatest"                % "3.2.14"            % "test,it",
    "org.pegdown"                  % "pegdown"                   % "1.6.0"             % "test,it",
    "org.scalatestplus.play"       %% "scalatestplus-play"       % "5.1.0"             % "test,it",
    "uk.gov.hmrc"                  %% "service-integration-test" % "1.2.0-play-28"     % "test,it",
    "com.typesafe.play"            %% "play-test"                % PlayVersion.current % "test,it",
    "com.vladsch.flexmark"         % "flexmark-all"              % "0.64.0"            % "test,it",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"     % "2.12.5"            % "test,it",
    "com.typesafe.play"            %% "play-akka-http-server"    % "2.8.7"             % "test,it",
    "com.typesafe.akka"            %% "akka-testkit"             % AkkaVersion         % Test,
    "org.skyscreamer"              % "jsonassert"                % "1.5.0"             % "it"
  )

  val overrides = Set()

}
