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

import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracerTestUtil;

import java.util.Collection;
import java.util.List;
import org.hamcrest.Matchers;
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
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.Assert;

/**
 * @author Gilles Robert
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {RabbitMqSendAndReceiveTracingItTest.TestConfig.class}
)
@RunWith(SpringJUnit4ClassRunner.class)
public class RabbitMqSendAndReceiveTracingItTest extends AbstractTestExecutionListener {

  private static final int PORT = 5672;
  @Autowired private RabbitTemplate rabbitTemplate;
  @Autowired private MockTracer tracer;
  @Autowired private SimpleMessageListenerContainer simpleMessageListenerContainer;
  @Autowired private CachingConnectionFactory cachingConnectionFactory;
  @Autowired private RabbitAdmin rabbitAdmin;

  private EmbeddedRabbitMq rabbitMq;

  @Override
  public void beforeTestClass(TestContext testContext) {
    EmbeddedRabbitMqConfig config =
        new EmbeddedRabbitMqConfig.Builder()
            .rabbitMqServerInitializationTimeoutInMillis(300000)
            .port(PORT)
            .build();
    rabbitMq = new EmbeddedRabbitMq(config);
    rabbitMq.start();
  }

  @Override
  public void afterTestClass(TestContext testContext) {
    rabbitAdmin.deleteExchange("myExchange");
    cachingConnectionFactory.destroy();
    simpleMessageListenerContainer.destroy();
    rabbitMq.stop();
  }

  @Before
  public void setup() {
    tracer.reset();
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

  @Test
  public void testSendAndReceiveRabbitMessage_whenParentSpanIsPresent() {
    Span span = tracer.buildSpan("parentOperation").start();
    tracer.scopeManager().activate(span, false);

    final String message = "hello world message!";
    rabbitTemplate.convertAndSend("myExchange", "#", message);

    await()
        .until(
            () -> {
              List<MockSpan> mockSpans = tracer.finishedSpans();
              return (mockSpans.size() == 2);
            });

    MockSpan.MockContext context = (MockSpan.MockContext) tracer.activeSpan().context();
    long parentSpanId = context.spanId();
    List<MockSpan> spans = tracer.finishedSpans();
    assertEquals(2, spans.size());
    MockSpan mockSentSpan = spans.get(0);
    assertThat(mockSentSpan.parentId(), Matchers.equalTo(parentSpanId));
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
  @EnableAspectJAutoProxy(proxyTargetClass = true)
  static class TestConfig {

    @Autowired @Lazy private Tracer tracer;

    @Bean
    public MockTracer mockTracer() {
      GlobalTracerTestUtil.resetGlobalTracer();
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
      rabbitConnectionFactoryBean.setUsername("guest");
      rabbitConnectionFactoryBean.setPassword("guest");
      rabbitConnectionFactoryBean.setHost("localhost");
      rabbitConnectionFactoryBean.setVirtualHost("/");
      rabbitConnectionFactoryBean.setPort(PORT);
      rabbitConnectionFactoryBean.afterPropertiesSet();
      return rabbitConnectionFactoryBean;
    }

    @Bean
    public CachingConnectionFactory cachingConnectionFactory(
        RabbitConnectionFactoryBean rabbitConnectionFactoryBean) throws Exception {
      return new CachingConnectionFactory(rabbitConnectionFactoryBean.getObject());
    }

    @Bean
    public RabbitAdmin rabbitAdmin(Queue queue, CachingConnectionFactory cachingConnectionFactory) {
      final TopicExchange exchange = new TopicExchange("myExchange", true, false);

      final RabbitAdmin admin = new RabbitAdmin(cachingConnectionFactory);
      admin.declareQueue(queue);
      admin.declareExchange(exchange);
      admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("#"));

      return admin;
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(
        CachingConnectionFactory cachingConnectionFactory, Queue queue) {
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
}
