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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import io.opentracing.mock.MockSpan;

import java.util.List;

import org.junit.Test;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


/**
 * Test tracing with manual configuration provided.
 * @author Gilles Robert
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = RabbitMqTracingManualConfigurationItTest.TestConfig.class
)
public class RabbitMqTracingManualConfigurationItTest extends BaseRabbitMqTracingItTest {

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Test
  public void send_withoutExchangeAndRoutingKey_shouldBeTraced() {
    // given
    Message requestMessage = createMessage();

    // when
    rabbitTemplate.send(requestMessage);

    // then
    assertConsumerAndProducerSpans(0, EMPTY_ROUTING_KEY);
  }

  @Test
  public void send_withExchangeAndRoutingKey_shouldBeTraced() {
    // given
    Message requestMessage = createMessage();

    // when
    rabbitTemplate.send(EXCHANGE, ROUTING_KEY, requestMessage);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
  }

  @Test
  public void send_withRoutingKeyAndWithoutExchange_shouldBeTraced() {
    // given
    Message requestMessage = createMessage();

    // when
    rabbitTemplate.send(ROUTING_KEY, requestMessage);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
  }

  @Test
  public void convertAndSend_withMessage_shouldBeTraced() {
    // given

    // when
    rabbitTemplate.convertAndSend(MESSAGE);

    //then
    assertConsumerAndProducerSpans(0, EMPTY_ROUTING_KEY);
  }

  @Test
  public void convertAndSend_withExchangeAndRoutingKey_shouldBeTraced() {
    // given

    // when
    rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, MESSAGE);

    //then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
  }

  @Test
  public void convertAndSend_withoutExchangeWithRoutingKey_shouldBeTraced() {
    // given

    // when
    rabbitTemplate.convertAndSend(ROUTING_KEY, MESSAGE);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
  }

  @Test
  public void convertAndSend_withMessagePostProcessor_shouldBeTraced() {
    // given
    Message requestMessage = createMessage();
    MessagePostProcessor messagePostProcessor = msg -> msg;

    // when
    rabbitTemplate.convertAndSend(requestMessage, messagePostProcessor);

    // then
    assertConsumerAndProducerSpans(0, EMPTY_ROUTING_KEY);
  }

  @Test
  public void convertAndSend_withRoutingKeyAndMessagePostProcessor_shouldBeTraced() {
    // given
    Message requestMessage = createMessage();
    MessagePostProcessor messagePostProcessor = msg -> msg;

    // when
    rabbitTemplate.convertAndSend(ROUTING_KEY, requestMessage, messagePostProcessor);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
  }

  @Test
  public void convertAndSend_withExchangeRoutingKeyAndMessagePostProcessor_shouldBeTraced() {
    // given
    MessagePostProcessor messagePostProcessor = msg -> msg;

    // when
    rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, MESSAGE, messagePostProcessor);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
  }

  @Test
  public void sendAndReceive_withoutExchangeAndRoutingKey_shouldBeTraced() {
    // given
    Message requestMessage = createMessage();

    // when
    Message response = rabbitTemplate.sendAndReceive(requestMessage);

    // then
    assertConsumerAndProducerSpans(0, EMPTY_ROUTING_KEY);
    assertOnResponse(response);
  }

  @Test
  public void sendAndReceive_withRoutingKeyAndMessage_shouldBeTraced() {
    // given
    Message requestMessage = createMessage();

    // when
    Message response = rabbitTemplate.sendAndReceive(ROUTING_KEY, requestMessage);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
    assertOnResponse(response);
  }

  @Test
  public void sendAndReceive_withExchangeRoutingKeyAndMessage_shouldBeTraced() {
    // given
    Message requestMessage = createMessage();

    // when
    Message response = rabbitTemplate.sendAndReceive(EXCHANGE, ROUTING_KEY, requestMessage);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
    assertOnResponse(response);
  }

  @Test
  public void sendAndReceive_withExchangeRoutingKeyMessageAndCorrelationData_shouldBeTraced() {
    // given
    Message requestMessage = createMessage();

    // when
    Message response = rabbitTemplate.sendAndReceive(EXCHANGE, ROUTING_KEY, requestMessage, null);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
    assertOnResponse(response);
  }

  @Test
  public void sendAndReceive_withProcessedLongerThanRabbitTemplateTimeout_shouldProduceSpanWithError() {
    // given
    Message requestMessage = createMessage();
    requestMessage.getMessageProperties().setHeader(RabbitWithoutRabbitTemplateConfig.TestMessageListener.HEADER_SLEEP_MILLIS,
        RabbitWithRabbitTemplateConfig.RABBIT_TEMPLATE_REPLY_TIMEOUT_MILLIS + 500);

    // when
    Message response = rabbitTemplate.sendAndReceive(EXCHANGE, ROUTING_KEY, requestMessage, null);

    // then
    assertThat(response, nullValue()); // response is null in case of timeout

    List<MockSpan> nowFinishedSpans = tracer.finishedSpans();
    assertThat(nowFinishedSpans.size(), equalTo(1)); // only send span should be finished, consumer is still sleeping

    MockSpan sendSpan = nowFinishedSpans.get(0);
    assertSpanRabbitTags(sendSpan, RabbitMqTracingTags.SPAN_KIND_PRODUCER, ROUTING_KEY);

    assertErrorTag(sendSpan); // check that when response wasn't sent before timeout,
    // then error tag is added (so span of the trace could be highlighted in UI)

    MockSpan receiveSpan = awaitFinishedSpans().getReceiveSpan();
    assertThat(REASON, receiveSpan, notNullValue());
    assertRabbitConsumerSpan(receiveSpan, ROUTING_KEY);
  }

  @Test
  public void convertSendAndReceive_withMessage_shouldBeTraced() {
    // given

    // when
    Object response = rabbitTemplate.convertSendAndReceive(MESSAGE);

    // then
    assertConsumerAndProducerSpans(0, EMPTY_ROUTING_KEY);
    assertOnResponse(response);
  }

  @Test
  public void convertSendAndReceive_withRoutingKeyAndMessage_shouldBeTraced() {
    // given

    // when
    Object response = rabbitTemplate.convertSendAndReceive(ROUTING_KEY, MESSAGE);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
    assertOnResponse(response);
  }

  @Test
  public void convertSendAndReceive_withExchangeRoutingKeyAndMessage_shouldBeTraced() {
    // given

    // when
    Object response = rabbitTemplate.convertSendAndReceive(EXCHANGE, ROUTING_KEY, MESSAGE);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
    assertOnResponse(response);
  }

  @Test
  public void convertSendAndReceive_withMessageAndMessagePostProcessor_shouldBeTraced() {
    // given
    Message requestMessage = createMessage();
    MessagePostProcessor messagePostProcessor = msg -> msg;

    // when
    Object response = rabbitTemplate.convertSendAndReceive(requestMessage, messagePostProcessor);

    // then
    assertConsumerAndProducerSpans(0, EMPTY_ROUTING_KEY);
    assertOnResponse(response);
  }

  @Test
  public void convertSendAndReceive_withRoutingKeyMessageAndMessagePostProcessor_shouldBeTraced() {
    // given
    Message requestMessage = createMessage();
    MessagePostProcessor messagePostProcessor = msg -> msg;

    // when
    Object response = rabbitTemplate.convertSendAndReceive(ROUTING_KEY, requestMessage, messagePostProcessor);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
    assertOnResponse(response);
  }

  @Test
  public void convertSendAndReceive_withExchangeRoutingKeyMessageMessagePostProcessor_shouldBeTraced() {
    // given
    MessagePostProcessor messagePostProcessor = msg -> msg;

    // when
    Object response = rabbitTemplate.convertSendAndReceive(EXCHANGE, ROUTING_KEY, MESSAGE, messagePostProcessor);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
    assertOnResponse(response);
  }

  @Test
  public void convertSendAndReceive_withExchangeRoutingKeyMessageMessagePostProcessorAndCorrelationData_shouldBeTraced() {
    // given
    MessagePostProcessor messagePostProcessor = msg -> msg;

    // when
    Object response = rabbitTemplate.convertSendAndReceive(EXCHANGE, ROUTING_KEY, MESSAGE, messagePostProcessor, null);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
    assertOnResponse(response);
  }

  private Message createMessage() {
    return rabbitTemplate.getMessageConverter().toMessage(MESSAGE, null);
  }

  private void assertOnResponse(Message response) {
    assertThat(response, notNullValue());
    assertThat(new String(response.getBody(), UTF_8), equalTo(
        RabbitWithoutRabbitTemplateConfig.TestMessageListener.REPLY_MSG_PREFIX + MESSAGE));
  }

  private void assertOnResponse(Object response) {
    assertThat(response, notNullValue());
    assertThat(response.toString(), equalTo(
        RabbitWithoutRabbitTemplateConfig.TestMessageListener.REPLY_MSG_PREFIX + MESSAGE));
  }

  @Configuration
  @Import({
      RabbitWithRabbitTemplateConfig.class,
      TracerConfig.class,
      RabbitMqTracingManualConfig.class
  })
  static class TestConfig {
  }
}
