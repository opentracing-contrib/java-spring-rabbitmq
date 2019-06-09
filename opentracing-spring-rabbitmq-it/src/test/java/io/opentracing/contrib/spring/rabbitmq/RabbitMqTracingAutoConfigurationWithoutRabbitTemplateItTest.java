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

import io.opentracing.contrib.spring.rabbitmq.RabbitTemplateProviderConfig.RabbitTemplateProvider;
import io.opentracing.contrib.spring.rabbitmq.RabbitWithoutRabbitTemplateConfig.TestMessageListener;
import io.opentracing.contrib.spring.rabbitmq.customizing.TracingRabbitTemplate;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.util.List;
import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Test tracing with default auto-configuration enabled and without RabbitTemplate bean.
 * @author Ats Uiboupin
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {RabbitMqTracingAutoConfigurationWithoutRabbitTemplateItTest.TestConfig.class}
)
public class RabbitMqTracingAutoConfigurationWithoutRabbitTemplateItTest extends BaseRabbitMqTracingItTest {

  @Autowired
  private RabbitTemplateProvider rabbitTemplateProvider;

  @Test
  public void convertAndSend_withExchangeAndRoutingKey_shouldBeTraced() {
    // given
    RabbitTemplate rabbitTemplate = getRabbitTemplate();

    // when
    rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, MESSAGE, (CorrelationData) null);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
  }

  @Test
  public void sendAndReceive_withExchangeAndRoutingKey_shouldBeTraced() {
    // given
    RabbitTemplate rabbitTemplate = getRabbitTemplate();
    Message requestMessage = rabbitTemplate.getMessageConverter().toMessage(MESSAGE, null);

    // when
    Message response = rabbitTemplate.sendAndReceive(EXCHANGE, ROUTING_KEY, requestMessage, null);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
    assertThat(new String(response.getBody(), UTF_8), equalTo(TestMessageListener.REPLY_MSG_PREFIX + MESSAGE));
  }

  @Test
  public void convertSendAndReceive_withExchangeAndRoutingKey_shouldBeTraced() {
    // given
    MessagePostProcessor messagePostProcessor = msg -> msg;
    RabbitTemplate rabbitTemplate = getRabbitTemplate();

    // when
    Object response = rabbitTemplate
        .convertSendAndReceive(EXCHANGE, ROUTING_KEY, MESSAGE, messagePostProcessor, null);

    // then
    assertConsumerAndProducerSpans(0, ROUTING_KEY);
    assertThat(response.toString(), equalTo(TestMessageListener.REPLY_MSG_PREFIX + MESSAGE));
  }

  @Test
  public void sendAndReceive_withMessageProcessedLongerThanRabbitTemplateTimeout_shouldProduceSpanWithError() {
    // given
    RabbitTemplate rabbitTemplate = getRabbitTemplate();
    Message requestMessage = rabbitTemplate.getMessageConverter().toMessage(MESSAGE, null);
    requestMessage.getMessageProperties().setHeader(TestMessageListener.HEADER_SLEEP_MILLIS,
        RabbitWithRabbitTemplateConfig.RABBIT_TEMPLATE_REPLY_TIMEOUT_MILLIS + 500);

    // when
    Message response = rabbitTemplate.sendAndReceive(EXCHANGE, ROUTING_KEY, requestMessage, null);

    // then
    // response is null in case of timeout
    assertThat(response, nullValue());
    List<MockSpan> nowFinishedSpans = tracer.finishedSpans();
    assertThat(nowFinishedSpans.size(), equalTo(1)); // only send span should be finished,
    // consumer is still sleeping
    MockSpan sendSpan = nowFinishedSpans.get(0);
    assertSpanRabbitTags(sendSpan, RabbitMqTracingTags.SPAN_KIND_PRODUCER, ROUTING_KEY);
    // check that when response wasn't sent before timeout,
    // then error tag is added (so span of the trace could be highlighted in UI)
    assertErrorTag(sendSpan);

    MockSpan receiveSpan = awaitFinishedSpans().getReceiveSpan();
    assertThat(REASON, receiveSpan, notNullValue());
    assertRabbitConsumerSpan(receiveSpan, ROUTING_KEY);
  }

  private RabbitTemplate getRabbitTemplate() {
    return rabbitTemplateProvider.getRabbitTemplate();
  }

  // TODO test timeout

  @Configuration
  @Import({
      RabbitWithoutRabbitTemplateConfig.class,
      TracerConfig.class,
  })
  @ImportAutoConfiguration(classes = {
      RabbitMqTracingAutoConfiguration.class // using auto-configuration for tracing RabbitMq
  })
  static class TestConfig {

    @Bean
    RabbitTemplateProvider rabbitTemplateProvider(RabbitConnectionFactoryBean rabbitConnectionFactoryBean,
        RabbitMqSpanDecorator spanDecorator, MockTracer tracer) throws Exception {

      CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(rabbitConnectionFactoryBean.getObject());

      TracingRabbitTemplate rabbitTemplate = new TracingRabbitTemplate(cachingConnectionFactory, tracer, spanDecorator);
      rabbitTemplate.setUseDirectReplyToContainer(false);

      RabbitWithRabbitTemplateConfig.configureRabbitTemplate(rabbitTemplate);

      return new RabbitTemplateProvider(rabbitTemplate);
    }
  }
}
