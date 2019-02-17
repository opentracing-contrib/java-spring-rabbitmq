/**
 * Copyright 2017-2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.rabbitmq.testrule;

import static io.opentracing.contrib.spring.rabbitmq.RabbitWithoutRabbitTemplateConfig.PORT;

import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import io.arivera.oss.embedded.rabbitmq.helpers.StartupException;

import java.util.concurrent.TimeUnit;
import org.junit.rules.ExternalResource;

public class TestRabbitServerResource extends ExternalResource {
  private EmbeddedRabbitMq rabbitMq;

  @Override
  protected void before() {
    EmbeddedRabbitMqConfig config =
        new EmbeddedRabbitMqConfig.Builder()
            .rabbitMqServerInitializationTimeoutInMillis(300000)
            .defaultRabbitMqCtlTimeoutInMillis(TimeUnit.SECONDS.toMillis(8))
            .port(PORT)
            .build();
    rabbitMq = new EmbeddedRabbitMq(config);
    try {
      rabbitMq.start();
    } catch (StartupException e) {
      throw new RuntimeException("Could not confirm RabbitMQ Server initialization completed successfully - perhaps real RabbitMQ server is running on port " + PORT + "?", e);
    }
  }

  @Override
  protected void after() {
    try {
      rabbitMq.stop();
    } catch (IllegalStateException e) {
      if (!"Stop shouldn't be called unless 'start()' was successful".equals(e.getMessage())) {
        throw e;
      }
    }
  }
}
