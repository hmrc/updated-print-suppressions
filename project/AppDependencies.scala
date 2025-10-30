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

  val hmrcMongo = "2.10.0"
  val bootstrapBackend = "9.19.0"
  val pekkoVersion: String = "1.0.3"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"         % bootstrapBackend,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-work-item-repo-play-30" % hmrcMongo,
    "uk.gov.hmrc"       %% "domain-play-30"                    % "12.1.0",
    "net.codingwell"    %% "scala-guice"                       % "6.0.0",
    "org.typelevel"     %% "cats-core"                         % "2.13.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"    % bootstrapBackend % "test",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30"   % hmrcMongo        % "test",
    "org.scalatestplus"      %% "mockito-3-4"               % "3.2.10.0"       % "test",
    "org.scalatestplus.play" %% "scalatestplus-play"        % "7.0.2"          % "test",
    // Core Pekko Stream testkit
    "org.apache.pekko"       %% "pekko-stream-testkit"      % pekkoVersion     % Test,
    // Classic actor testkit (for TestKit base class)
    "org.apache.pekko"       %% "pekko-testkit"             % pekkoVersion     % Test,
    "org.apache.pekko"       %% "pekko-actor-testkit-typed" % pekkoVersion     % Test
  )

  val it = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapBackend % "it/test",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongo        % "it/test"
  )
}
