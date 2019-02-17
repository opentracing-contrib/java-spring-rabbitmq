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
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
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
    classes = {RabbitMqTracingAutoConfigurationWithoutRabbitTemplateItTest.TestConfig.class}
)
public class RabbitMqTracingAutoConfigurationWithoutRabbitTemplateItTest extends BaseRabbitMqTracingItTest {
  private static final String ROUTING_KEY = "#";
  private static final String EXCHANGE = "myExchange";
  private static final String MESSAGE = "hello world message!";


  @Autowired private RabbitTemplateProvider rabbitTemplateProvider;

  @Test
  public void givenCustomRabbitTemplateImplementationWithTracingSupport_convertAndSend_shouldBeTraced() {
    getRabbitTemplate().convertAndSend(EXCHANGE, ROUTING_KEY, MESSAGE, (CorrelationData) null);

    assertConsumerAndProducerSpans(0);
  }

  private RabbitTemplate getRabbitTemplate() {
    return rabbitTemplateProvider.getRabbitTemplate();
  }

  @Test
  public void givenCustomRabbitTemplateImplementationWithTracingSupport_sendAndReceive_shouldBeTraced() {
    RabbitTemplate rabbitTemplate = getRabbitTemplate();
    Message requestMessage = rabbitTemplate.getMessageConverter().toMessage(MESSAGE, null);
    Message response = rabbitTemplate.sendAndReceive(EXCHANGE, ROUTING_KEY, requestMessage, null);

    assertConsumerAndProducerSpans(0);
    assertThat(new String(response.getBody(), UTF_8), equalTo(TestMessageListener.REPLY_MSG_PREFIX + MESSAGE));
  }

  @Test
  public void givenCustomRabbitTemplateImplementationWithTracingSupport_convertSendAndReceive_shouldBeTraced() {
    MessagePostProcessor messagePostProcessor = msg -> msg;
    Object response = getRabbitTemplate().convertSendAndReceive(EXCHANGE, ROUTING_KEY, MESSAGE, messagePostProcessor, null);

    assertConsumerAndProducerSpans(0);
    assertThat(response.toString(), equalTo(TestMessageListener.REPLY_MSG_PREFIX + MESSAGE));
  }

  @Test
  public void givenMessageThatIsProcessedLongerThanRabbitTemplateTimeout_sendAndReceive_shouldProduceSpanWithError() {
    final String message = "hello world message!";
    RabbitTemplate rabbitTemplate = getRabbitTemplate();
    Message requestMessage = rabbitTemplate.getMessageConverter().toMessage(message, null);
    requestMessage.getMessageProperties().setHeader(TestMessageListener.HEADER_SLEEP_MILLIS, RabbitWithRabbitTemplateConfig.RABBIT_TEMPLATE_REPLY_TIMEOUT_MILLIS + 500);
    Message response = rabbitTemplate.sendAndReceive(EXCHANGE, ROUTING_KEY, requestMessage, null);

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
    RabbitTemplateProvider rabbitTemplateProvider(
        RabbitConnectionFactoryBean rabbitConnectionFactoryBean,
        RabbitMqSpanDecorator spanDecorator,
        MockTracer tracer)
        throws Exception {
      final CachingConnectionFactory cachingConnectionFactory =
          new CachingConnectionFactory(rabbitConnectionFactoryBean.getObject());
      TracingRabbitTemplate rabbitTemplate = new TracingRabbitTemplate(cachingConnectionFactory, tracer, spanDecorator);
      RabbitWithRabbitTemplateConfig.configureRabbitTemplate(rabbitTemplate);
      return new RabbitTemplateProvider(rabbitTemplate);
    }
  }
}
