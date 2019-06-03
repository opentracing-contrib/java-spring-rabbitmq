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

import io.opentracing.contrib.spring.rabbitmq.RabbitWithoutRabbitTemplateConfig.TestMessageListener;
import io.opentracing.mock.MockSpan;
import java.util.List;
import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Ats Uiboupin
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {RabbitMqTracingAutoConfigurationItTest.TestConfig.class}
)
public class RabbitMqTracingAutoConfigurationItTest extends BaseRabbitMqTracingItTest {

  @Autowired private RabbitTemplate rabbitTemplate;

  @Test
  public void givenAutoConfiguredRabbitTemplate_send_shouldBeTraced() {
    final String message = "hello world message!";
    Message requestMessage = rabbitTemplate.getMessageConverter().toMessage(message, null);
    rabbitTemplate.send("myExchange", "#", requestMessage);

    assertConsumerAndProducerSpans(0);
  }

  @Test
  public void givenAutoConfiguredRabbitTemplate_convertAndSend_shouldBeTraced() {
    final String message = "hello world message!";
    rabbitTemplate.convertAndSend("myExchange", "#", message);

    assertConsumerAndProducerSpans(0);
  }

  @Test
  public void givenAutoConfiguredRabbitTemplate_convertAndSend_withMessagePostProcessor_shouldBeTraced() {
    final String message = "hello world message!";
    MessagePostProcessor messagePostProcessor = msg -> msg;
    rabbitTemplate.convertAndSend("myExchange", "#", message, messagePostProcessor);

    assertConsumerAndProducerSpans(0);
  }

  @Test
  public void givenAutoConfiguredRabbitTemplate_sendAndReceive_shouldBeTraced() {
    final String message = "hello world message!";
    Message requestMessage = rabbitTemplate.getMessageConverter().toMessage(message, null);
    Message response = rabbitTemplate.sendAndReceive("myExchange", "#", requestMessage, null);

    assertConsumerAndProducerSpans(0);
    assertThat(new String(response.getBody(), UTF_8), equalTo(TestMessageListener.REPLY_MSG_PREFIX + message));
  }

  @Test
  public void givenAutoConfiguredRabbitTemplate_convertSendAndReceive_shouldBeTraced() {
    final String message = "hello world message!";
    MessagePostProcessor messagePostProcessor = msg -> msg;
    Object response = rabbitTemplate.convertSendAndReceive("myExchange", "#", message, messagePostProcessor, null);

    assertConsumerAndProducerSpans(0);
    assertThat(response.toString(), equalTo(TestMessageListener.REPLY_MSG_PREFIX + message));
  }

  @Test
  public void givenMessageThatIsProcessedLongerThanRabbitTemplateTimeout_sendAndReceive_shouldProduceSpanWithError() {
    final String message = "hello world message!";
    Message requestMessage = rabbitTemplate.getMessageConverter().toMessage(message, null);
    requestMessage.getMessageProperties().setHeader(TestMessageListener.HEADER_SLEEP_MILLIS, RabbitWithRabbitTemplateConfig.RABBIT_TEMPLATE_REPLY_TIMEOUT_MILLIS + 500);
    Message response = rabbitTemplate.sendAndReceive("myExchange", "#", requestMessage, null);

    // response is null in case of timeout
    assertThat(response, nullValue());
    List<MockSpan> nowFinishedSpans = tracer.finishedSpans();
    assertThat(nowFinishedSpans.size(), equalTo(1)); // only send span should be finished, consumer is still sleeping
    MockSpan sendSpan = nowFinishedSpans.get(0);
    assertSpanRabbitTags(sendSpan, RabbitMqTracingTags.SPAN_KIND_PRODUCER);
    // check that when response wasn't sent before timeout, then error tag is added (so span of the trace could be highlighted in UI)
    assertErrorTag(sendSpan);

    MockSpan receiveSpan = awaitFinishedSpans().getReceiveSpan();
    assertThat("Didn't receive receive span (perhaps consumer slept too long or didn't wait enough time to receive receive span?)", receiveSpan, notNullValue());
    assertRabbitConsumerSpan(receiveSpan);
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
  }
}
