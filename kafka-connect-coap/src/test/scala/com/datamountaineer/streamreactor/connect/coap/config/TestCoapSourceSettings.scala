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

package com.datamountaineer.streamreactor.connect.coap.config

import com.datamountaineer.streamreactor.connect.coap.TestBase
import com.datamountaineer.streamreactor.connect.coap.configs.{CoapConfig, CoapSettings}
import org.apache.kafka.common.config.ConfigException
import org.scalatest.WordSpec

/**
  * Created by andrew@datamountaineer.com on 28/12/2016. 
  * stream-reactor
  */
class TestCoapSourceSettings extends WordSpec with TestBase {
  "should create CoapSettings for an insecure connection" in {
    val props = getPropsInsecure
    val config = CoapConfig(props)
    val settings = CoapSettings(config, sink = false)
    val setting = settings.head
    setting.kcql.getSource shouldBe RESOURCE_INSECURE
    setting.kcql.getTarget shouldBe TOPIC
    setting.uri shouldBe SOURCE_URI_INSECURE
    setting.keyStoreLoc.nonEmpty shouldBe false
    setting.trustStoreLoc.nonEmpty shouldBe false
  }

  "should create CoapSettings for an secure connection" in {
    val props = getPropsSecure
    val config = CoapConfig(props)
    val settings = CoapSettings(config, sink = false)
    val setting = settings.head
    setting.kcql.getSource shouldBe RESOURCE_SECURE
    setting.kcql.getTarget shouldBe TOPIC
    setting.uri shouldBe SOURCE_URI_SECURE
    setting.keyStoreLoc.nonEmpty shouldBe true
    setting.trustStoreLoc.nonEmpty shouldBe true
  }

  "should fail to create CoapSettings for an secure connection with key wrong path" in {
    val props = getPropsSecureKeyNotFound
    val config = CoapConfig(props)
    intercept[ConfigException] {
      CoapSettings(config, sink = false)
    }
  }

  "should fail to create CoapSettings for an secure connection with trust wrong path" in {
    val props = getPropsSecureTrustNotFound
    val config = CoapConfig(props)
    intercept[ConfigException] {
      CoapSettings(config, sink = false)
    }
  }
}
