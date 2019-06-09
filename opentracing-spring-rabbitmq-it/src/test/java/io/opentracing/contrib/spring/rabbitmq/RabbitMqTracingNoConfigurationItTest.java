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
package io.opentracing.contrib.spring.rabbitmq;

import org.junit.Test;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Test tracing using neither manual nor auto-configuration for tracing RabbitMq.
 * @author Ats Uiboupin
 * @author Gilles Robert
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = RabbitMqTracingNoConfigurationItTest.TestConfig.class
)
public class RabbitMqTracingNoConfigurationItTest extends BaseRabbitMqTracingItTest {

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Test
  public void send_withoutExchangeAndRoutingKey_shouldNotBeTraced() {
    // given
    Message requestMessage = createMessage();

    // when
    rabbitTemplate.send(requestMessage);

    // then
    checkNoSpans();
  }

  @Test
  public void send_withExchangeAndRoutingKey_shouldNotBeTraced() {
    // given
    Message requestMessage = createMessage();

    // when
    rabbitTemplate.send(EXCHANGE, ROUTING_KEY, requestMessage);

    // then
    checkNoSpans();
  }

  @Test
  public void send_withRoutingKeyAndWithoutExchange_shouldNotBeTraced() {
    // given
    Message requestMessage = createMessage();

    // when
    rabbitTemplate.send(ROUTING_KEY, requestMessage);

    // then
    checkNoSpans();
  }

  @Test
  public void convertAndSend_withMessage_shouldNotBeTraced() {
    // given

    // when
    rabbitTemplate.convertAndSend(MESSAGE);

    //then
    checkNoSpans();
  }

  @Test
  public void convertAndSend_withExchangeAndRoutingKey_shouldNotBeTraced() {
    // given

    // when
    rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, MESSAGE);

    //then
    checkNoSpans();
  }

  @Test
  public void convertAndSend_withoutExchangeWithRoutingKey_shouldNotBeTraced() {
    // given

    // when
    rabbitTemplate.convertAndSend(ROUTING_KEY, MESSAGE);

    // then
    checkNoSpans();
  }

  @Test
  public void convertAndSend_withMessagePostProcessor_shouldNotBeTraced() {
    // given
    Message requestMessage = createMessage();
    MessagePostProcessor messagePostProcessor = msg -> msg;

    // when
    rabbitTemplate.convertAndSend(requestMessage, messagePostProcessor);

    // then
    checkNoSpans();
  }

  @Test
  public void convertAndSend_withRoutingKeyAndMessagePostProcessor_shouldNotBeTraced() {
    // given
    Message requestMessage = createMessage();
    MessagePostProcessor messagePostProcessor = msg -> msg;

    // when
    rabbitTemplate.convertAndSend(ROUTING_KEY, requestMessage, messagePostProcessor);

    // then
    checkNoSpans();
  }

  @Test
  public void convertAndSend_withExchangeRoutingKeyAndMessagePostProcessor_shouldNotBeTraced() {
    // given
    MessagePostProcessor messagePostProcessor = msg -> msg;

    // when
    rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, MESSAGE, messagePostProcessor);

    // then
    checkNoSpans();
  }

  @Test
  public void sendAndReceive_withoutExchangeAndRoutingKey_shouldNotBeTraced() {
    // given
    Message requestMessage = createMessage();

    // when
    Message response = rabbitTemplate.sendAndReceive(requestMessage);

    // then
    checkNoSpans();
  }

  @Test
  public void sendAndReceive_withRoutingKeyAndMessage_shouldNotBeTraced() {
    // given
    Message requestMessage = createMessage();

    // when
    rabbitTemplate.sendAndReceive(ROUTING_KEY, requestMessage);

    // then
    checkNoSpans();
  }

  @Test
  public void sendAndReceive_withExchangeRoutingKeyAndMessage_shouldNotBeTraced() {
    // given
    Message requestMessage = createMessage();

    // when
    rabbitTemplate.sendAndReceive(EXCHANGE, ROUTING_KEY, requestMessage);

    // then
    checkNoSpans();
  }

  @Test
  public void sendAndReceive_withExchangeRoutingKeyMessageAndCorrelationData_shouldNotBeTraced() {
    // given
    Message requestMessage = createMessage();

    // when
    rabbitTemplate.sendAndReceive(EXCHANGE, ROUTING_KEY, requestMessage, null);

    // then
    checkNoSpans();
  }

  @Test
  public void sendAndReceive_withProcessedLongerThanRabbitTemplateTimeout_shouldProduceSpanWithError() {
    // given
    Message requestMessage = createMessage();
    requestMessage.getMessageProperties().setHeader(RabbitWithoutRabbitTemplateConfig.TestMessageListener.HEADER_SLEEP_MILLIS,
        RabbitWithRabbitTemplateConfig.RABBIT_TEMPLATE_REPLY_TIMEOUT_MILLIS + 500);

    // when
    rabbitTemplate.sendAndReceive(EXCHANGE, ROUTING_KEY, requestMessage, null);

    // then
    checkNoSpans();
  }

  @Test
  public void convertSendAndReceive_withMessage_shouldNotBeTraced() {
    // given

    // when
    rabbitTemplate.convertSendAndReceive(MESSAGE);

    // then
    checkNoSpans();
  }

  @Test
  public void convertSendAndReceive_withRoutingKeyAndMessage_shouldNotBeTraced() {
    // given

    // when
    rabbitTemplate.convertSendAndReceive(ROUTING_KEY, MESSAGE);

    // then
    checkNoSpans();
  }

  @Test
  public void convertSendAndReceive_withExchangeRoutingKeyAndMessage_shouldNotBeTraced() {
    // given

    // when
    rabbitTemplate.convertSendAndReceive(EXCHANGE, ROUTING_KEY, MESSAGE);

    // then
    checkNoSpans();
  }

  @Test
  public void convertSendAndReceive_withMessageAndMessagePostProcessor_shouldNotBeTraced() {
    // given
    Message requestMessage = createMessage();
    MessagePostProcessor messagePostProcessor = msg -> msg;

    // when
    rabbitTemplate.convertSendAndReceive(requestMessage, messagePostProcessor);

    // then
    checkNoSpans();
  }

  @Test
  public void convertSendAndReceive_withRoutingKeyMessageAndMessagePostProcessor_shouldNotBeTraced() {
    // given
    Message requestMessage = createMessage();
    MessagePostProcessor messagePostProcessor = msg -> msg;

    // when
    rabbitTemplate.convertSendAndReceive(ROUTING_KEY, requestMessage, messagePostProcessor);

    // then
    checkNoSpans();
  }

  @Test
  public void convertSendAndReceive_withExchangeRoutingKeyMessageMessagePostProcessor_shouldNotBeTraced() {
    // given
    MessagePostProcessor messagePostProcessor = msg -> msg;

    // when
    rabbitTemplate.convertSendAndReceive(EXCHANGE, ROUTING_KEY, MESSAGE, messagePostProcessor);

    // then
    checkNoSpans();
  }

  @Test
  public void convertSendAndReceive_withExchangeRoutingKeyMessageMessagePostProcessorAndCorrelationData_shouldNotBeTraced() {
    // given
    MessagePostProcessor messagePostProcessor = msg -> msg;

    // when
    rabbitTemplate.convertSendAndReceive(EXCHANGE, ROUTING_KEY, MESSAGE, messagePostProcessor, null);

    // then
    checkNoSpans();
  }

  private Message createMessage() {
    return rabbitTemplate.getMessageConverter().toMessage(MESSAGE, null);
  }

  @Configuration
  @Import({
      RabbitWithRabbitTemplateConfig.class,
      TracerConfig.class,
      // RabbitMqTracingManualConfig.class - using neither manual nor auto-configuration for tracing RabbitMq
  })
  static class TestConfig {
  }
}
