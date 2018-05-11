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

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.rabbitmq.client.LongString;
import com.rabbitmq.client.SaslMechanism;
import com.rabbitmq.client.impl.LongStringHelper;
import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

/**
 *  @author Gilles Robert
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {RabbitMqSendAndReceiveTracingItTest.TestConfig.class,
    RabbitMqSendAndReceiveTracingItTest.RabbitListenerTestConfig.class}
)
@RunWith(SpringJUnit4ClassRunner.class)
public class RabbitMqSendAndReceiveTracingItTest {

  private final EmbeddedQpidBroker broker = new EmbeddedQpidBroker();
  @Autowired private RabbitTemplate rabbitTemplate;
  @Autowired private MockTracer tracer;
  @Autowired private RabbitConnectionFactoryBean rabbitConnectionFactoryBean;
  @Autowired private Queue queue;

  @Before
  public void setup() throws Exception {
    tracer.reset();
    broker.start();

    final CachingConnectionFactory cachingConnectionFactory =
        new CachingConnectionFactory(rabbitConnectionFactoryBean.getObject());

    final TopicExchange exchange = new TopicExchange("myExchange", true, false);

    final RabbitAdmin admin = new RabbitAdmin(cachingConnectionFactory);
    admin.declareQueue(queue);
    admin.declareExchange(exchange);
    admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("#"));

    cachingConnectionFactory.destroy();
  }

  @After
  public void deleteExchange() throws Exception {
    final CachingConnectionFactory cachingConnectionFactory =
        new CachingConnectionFactory(rabbitConnectionFactoryBean.getObject());
    final RabbitAdmin admin = new RabbitAdmin(cachingConnectionFactory);
    admin.deleteExchange("myExchange");
    cachingConnectionFactory.destroy();
    broker.stop();
  }

  @Test
  public void testSendAndReceiveRabbitMessage() {
    final String message = "hello world message!";
    rabbitTemplate.convertAndSend("myExchange", "#", message);

    await()
        .until(
            () -> {
              List<MockSpan> mockSpans = tracer.finishedSpans();
              return (mockSpans.size() == 2);
            });

    List<MockSpan> spans = tracer.finishedSpans();
    assertEquals(2, spans.size());
    MockSpan mockSentSpan = spans.get(0);
    assertThat(
        mockSentSpan.operationName(), Matchers.equalTo(RabbitMqTracingTags.SPAN_KIND_PRODUCER));
    assertThat(mockSentSpan.tags(), Matchers.notNullValue());
    assertThat(mockSentSpan.tags().size(), Matchers.is(5));
    assertThat(mockSentSpan.tags().get("messageid"), Matchers.notNullValue());
    assertThat(
        mockSentSpan.tags().get("component"),
        Matchers.equalTo(RabbitMqTracingTags.RABBITMQ.getKey()));
    assertThat(mockSentSpan.tags().get("exchange"), Matchers.equalTo("myExchange"));
    assertThat(
        mockSentSpan.tags().get("span.kind"),
        Matchers.equalTo(RabbitMqTracingTags.SPAN_KIND_PRODUCER));
    assertThat(mockSentSpan.tags().get("routingkey"), Matchers.equalTo("#"));

    MockSpan mockReceivedSpan = spans.get(1);
    assertThat(
        mockReceivedSpan.operationName(), Matchers.equalTo(RabbitMqTracingTags.SPAN_KIND_CONSUMER));
    assertThat(mockReceivedSpan.tags(), Matchers.notNullValue());
    assertThat(mockReceivedSpan.tags().size(), Matchers.is(6));
    assertThat(mockReceivedSpan.tags().get("messageid"), Matchers.notNullValue());
    assertThat(
        mockReceivedSpan.tags().get("component"),
        Matchers.equalTo(RabbitMqTracingTags.RABBITMQ.getKey()));
    assertThat(mockReceivedSpan.tags().get("exchange"), Matchers.equalTo("myExchange"));
    assertThat(
        mockReceivedSpan.tags().get("span.kind"),
        Matchers.equalTo(RabbitMqTracingTags.SPAN_KIND_CONSUMER));
    assertThat(mockReceivedSpan.tags().get("routingkey"), Matchers.equalTo("#"));
    assertThat(mockReceivedSpan.tags().get("consumerqueue"), Matchers.equalTo("myQueue"));
    assertThat(mockReceivedSpan.generatedErrors().size(), Matchers.is(0));
    assertSameTraceId(spans);
  }

  private void assertSameTraceId(Collection<MockSpan> spans) {
    if (!spans.isEmpty()) {
      final long traceId = spans.iterator().next().context().traceId();
      for (MockSpan span : spans) {
        assertEquals(traceId, span.context().traceId());
      }
    }
  }

  @Configuration
  static class RabbitListenerTestConfig {

    @Autowired private RabbitConnectionFactoryBean rabbitConnectionFactoryBean;
    @Autowired private Queue queue;

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer() throws Exception {
      final CachingConnectionFactory cachingConnectionFactory =
          new CachingConnectionFactory(rabbitConnectionFactoryBean.getObject());
      SimpleMessageListenerContainer container =
          new SimpleMessageListenerContainer(cachingConnectionFactory);
      container.setQueues(queue);
      container.setMessageListener(new MessageListenerAdapter(new MessageListenerTest()));

      return container;
    }

    class MessageListenerTest {

      public void handleMessage(Object message) {}
    }
  }

  @Configuration
  static class TestConfig {

    @Autowired @Lazy private Tracer tracer;

    private static void resetGlobalTracer() {
      try {
        Field globalTracerField = GlobalTracer.class.getDeclaredField("tracer");
        globalTracerField.setAccessible(true);
        globalTracerField.set(null, NoopTracerFactory.create());
        globalTracerField.setAccessible(false);
      } catch (Exception e) {
        throw new RuntimeException("Error reflecting globalTracer: " + e.getMessage(), e);
      }
    }

    @Bean
    public MockTracer mockTracer() {
      resetGlobalTracer();
      return new MockTracer();
    }

    @Bean
    public RabbitMqSendTracingAspect rabbitMqSendTracingAspect(RabbitTemplate rabbitTemplate) {
      Assert.notNull(
          rabbitTemplate.getMessageConverter(),
          "RabbitTemplate has no message converter configured");
      return new RabbitMqSendTracingAspect(tracer, rabbitTemplate.getMessageConverter());
    }

    @Bean
    public RabbitMqReceiveTracingInterceptor rabbitMqReceiveTracingInterceptor() {
      return new RabbitMqReceiveTracingInterceptor(tracer);
    }

    @Bean
    public RabbitMqBeanPostProcessor rabbitMqBeanPostProcessor(
        RabbitMqReceiveTracingInterceptor interceptor) {
      return new RabbitMqBeanPostProcessor(interceptor);
    }

    @Bean
    public Queue queue() {
      return new Queue("myQueue", false);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(RabbitConnectionFactoryBean rabbitConnectionFactoryBean)
        throws Exception {
      final CachingConnectionFactory cachingConnectionFactory =
          new CachingConnectionFactory(rabbitConnectionFactoryBean.getObject());
      SimpleMessageConverter messageConverter = new SimpleMessageConverter();
      messageConverter.setCreateMessageIds(true);
      RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
      rabbitTemplate.setMessageConverter(messageConverter);
      return rabbitTemplate;
    }

    @Bean
    public RabbitConnectionFactoryBean rabbitConnectionFactoryBean() throws Exception {
      RabbitConnectionFactoryBean rabbitConnectionFactoryBean = new RabbitConnectionFactoryBean();
      rabbitConnectionFactoryBean.setUsername("admin");
      rabbitConnectionFactoryBean.setPassword("admin");
      rabbitConnectionFactoryBean.setPort(EmbeddedQpidBroker.BROKER_PORT);
      rabbitConnectionFactoryBean.setSkipServerCertificateValidation(true);
      rabbitConnectionFactoryBean.setSaslConfig(
          strings -> new SaslMechanism() {
            @Override
            public String getName() {
              return "PLAIN";
            }

            @Override
            public LongString handleChallenge(
                LongString longString, String username, String password) {
                return LongStringHelper.asLongString("\u0000" + username + "\u0000" + password);
              }
            });
      rabbitConnectionFactoryBean.afterPropertiesSet();
      return rabbitConnectionFactoryBean;
    }
  }
}
