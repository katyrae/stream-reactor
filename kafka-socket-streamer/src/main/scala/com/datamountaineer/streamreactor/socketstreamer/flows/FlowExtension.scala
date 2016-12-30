/*
 * *
 *   * Copyright 2016 Datamountaineer.
 *   *
 *   * Licensed under the Apache License, Version 2.0 (the "License");
 *   * you may not use this file except in compliance with the License.
 *   * You may obtain a copy of the License at
 *   *
 *   * http://www.apache.org/licenses/LICENSE-2.0
 *   *
 *   * Unless required by applicable law or agreed to in writing, software
 *   * distributed under the License is distributed on an "AS IS" BASIS,
 *   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   * See the License for the specific language governing permissions and
 *   * limitations under the License.
 *   *
 */

package com.datamountaineer.streamreactor.socketstreamer.flows

import akka.stream.ThrottleMode.Shaping
import akka.stream.scaladsl.{Flow, Source}
import com.typesafe.scalalogging.slf4j.StrictLogging

import scala.concurrent.duration._

object FlowExtension extends StrictLogging {

  implicit class FlowRich[-In, +Out, +Mat](val flow: Flow[In, Out, Mat]) extends AnyVal {
    def withSampling(count: Int, rate: Int): Flow[In, Out, Mat] = {
      flow
        .conflateWithSeed(Vector(_)) {
          case (buff, m) => if (buff.size < count) buff :+ m else buff
        }
        .throttle(1, rate.millis, 1, Shaping)
        .mapConcat(identity)
    }
  }

}
