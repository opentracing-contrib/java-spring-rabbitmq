/**
 * Copyright 2017-2018 The OpenTracing Authors
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
package io.opentracing.contrib.spring.rabbitmq;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import io.opentracing.contrib.spring.rabbitmq.customizing.CustomizedRabbitMqSpanDecorator;
import io.opentracing.mock.MockSpan;

import java.util.List;
import org.junit.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Ats Uiboupin
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {RabbitMqTracingAutoConfigurationCustomizationItTest.TestConfig.class}
)
public class RabbitMqTracingAutoConfigurationCustomizationItTest extends BaseRabbitMqTracingItTest {

  @Autowired private RabbitTemplate rabbitTemplate;

  @Test
  public void testSendAndReceiveRabbitMessage_withCustomizedProducerSpan() {
    final String message = "hello world message!";
    rabbitTemplate.convertAndSend("myExchange", "#", message);

    List<MockSpan> spans = awaitFinishedSpans();
    MockSpan sentSpan = spans.get(0);
    MockSpan receiveSpan = spans.get(1);

    assertSpanRabbitTags(sentSpan, RabbitMqTracingTags.SPAN_KIND_PRODUCER);
    assertThat(sentSpan.operationName(), equalTo(CustomizedRabbitMqSpanDecorator.OVERRIDEN_OPERATION_NAME_FOR_SENDING));

    assertSpanRabbitTags(receiveSpan, RabbitMqTracingTags.SPAN_KIND_CONSUMER);
    assertThat(receiveSpan.operationName(), equalTo(CustomizedRabbitMqSpanDecorator.OVERRIDEN_OPERATION_NAME_FOR_RECEIVING));
  }

  @Configuration
  @Import({
      RabbitWithRabbitTemplateConfig.class,
      TracerConfig.class,
  })
  @ImportAutoConfiguration(classes = {
      RabbitMqTracingAutoConfiguration.class // using auto-configuration for tracing RabbitMq
  })
  static class TestConfig {
    @Bean
    public RabbitMqSpanDecorator rabbitMqSpanDecorator() {
      return new CustomizedRabbitMqSpanDecorator();
    }

  }

}
