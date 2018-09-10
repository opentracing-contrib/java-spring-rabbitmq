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

import static io.opentracing.contrib.spring.rabbitmq.RabbitWithoutRabbitTemplateConfig.PORT;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import io.arivera.oss.embedded.rabbitmq.helpers.StartupException;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gilles Robert
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {RabbitMqSendAndReceiveTracingItTest.TestConfig.class}
)
@RunWith(SpringJUnit4ClassRunner.class)
public class RabbitMqSendAndReceiveTracingItTest {

  @Autowired private RabbitTemplate rabbitTemplate;
  @Autowired private MockTracer tracer;

  private static EmbeddedRabbitMq rabbitMq;

  @BeforeClass
  public static void beforeClass() {
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

  @AfterClass
  public static void tearDown() {
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
  @Import({
      RabbitWithRabbitTemplateConfig.class,
      TracerConfig.class,
      RabbitMqTracingManualConfig.class
  })
  static class TestConfig {
  }
}
