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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import io.opentracing.contrib.spring.rabbitmq.util.FinishedSpansHelper;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;

import java.util.List;
import java.util.Map;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * @author Ats Uiboupin
 * @author Gilles Robert
 */
@RunWith(SpringJUnit4ClassRunner.class)
// Needed to destroy rabbit message listeners (created by RabbitWithoutRabbitTemplateConfig.messageListenerContainer)
// to avoid race condition errors related to listeners from previous test applicationContext grabbing messages
// of next test that results in rabbit-receive span being added to MockTracer of previous test
// and hence next test never sees that span when checking finished spans.
@DirtiesContext
public abstract class BaseRabbitMqTracingItTest {

  static final String EXCHANGE = "myExchange";
  static final String ROUTING_KEY = "#";
  static final String EMPTY_ROUTING_KEY = "";
  static final String MESSAGE = "hello world message!";
  static final String REASON =
      "Didn't receive receive span (perhaps consumer slept too long or didn't wait enough time to receive receive span?)";

  private static final GenericContainer RABBIT =
      new GenericContainer("rabbitmq:3.7.14-alpine")
          .withExposedPorts(5672)
          .waitingFor(Wait.forListeningPort());

  static {
    RABBIT.start();

    System.setProperty("spring.rabbitmq.host", RABBIT.getContainerIpAddress());
    System.setProperty("spring.rabbitmq.port", String.valueOf(RABBIT.getMappedPort(5672)));
  }

  @Autowired
  protected MockTracer tracer;

  @Before
  public void setup() {
    tracer.reset();
  }

  void assertConsumerAndProducerSpans(long parentSpanId, String routingKey) {
    FinishedSpansHelper spans = awaitFinishedSpans();
    assertEquals(2, spans.getAllSpans().size());
    assertRabbitProducerSpan(spans.getSendSpan(), parentSpanId, routingKey);

    assertRabbitConsumerSpan(spans.getReceiveSpan(), routingKey);
    spans.assertSameTraceId();
  }

  void assertRabbitConsumerSpan(MockSpan mockReceivedSpan, String routingKey) {
    String expectedConsumerQueueName = "myQueue";
    assertRabbitSpan(mockReceivedSpan, RabbitMqTracingTags.SPAN_KIND_CONSUMER, 6, routingKey);
    assertThat(mockReceivedSpan.operationName(), equalTo(RabbitMqTracingTags.SPAN_KIND_CONSUMER));
    assertThat(mockReceivedSpan.tags().get("consumerqueue"), equalTo(expectedConsumerQueueName));
    assertThat(mockReceivedSpan.generatedErrors().size(), is(0));
  }

  void assertSpanRabbitTags(MockSpan mockSentSpan, String spanKind, String routingKey) {
    assertThat(mockSentSpan.tags().get("messageid"), notNullValue());
    assertThat(
        mockSentSpan.tags().get("component"),
        equalTo(RabbitMqTracingTags.RABBITMQ.getKey()));
    assertThat(mockSentSpan.tags().get("exchange"), equalTo("myExchange"));
    assertThat(
        mockSentSpan.tags().get("span.kind"),
        equalTo(spanKind));
    assertThat(mockSentSpan.tags().get("routingkey"), equalTo(routingKey));
  }

  void assertErrorTag(MockSpan span) {
    Map<String, Object> sendSpanTags = span.tags();
    assertThat(sendSpanTags.get(Tags.ERROR.getKey()), equalTo(true));
  }

  FinishedSpansHelper awaitFinishedSpans() {
    await()
        .timeout(Duration.TWO_SECONDS)
        .until(
            () -> {
              List<MockSpan> mockSpans = tracer.finishedSpans();
              return (mockSpans.size() == 2);
            });

    return new FinishedSpansHelper(tracer.finishedSpans());
  }

  void checkNoSpans() {
    try {
      FinishedSpansHelper spans = awaitFinishedSpans();
      Assert.fail("Expected not to receive trace spans when autoconfiguration isn't enabled, but got: " + spans);
    } catch (ConditionTimeoutException e) {
      assertThat(tracer.finishedSpans().size(), equalTo(0));
    }
  }

  private void assertRabbitProducerSpan(MockSpan mockSentSpan, long expectedParentSpanId, String routingKey) {
    assertThat(mockSentSpan.parentId(), equalTo(expectedParentSpanId));
    assertRabbitSpan(mockSentSpan, RabbitMqTracingTags.SPAN_KIND_PRODUCER, 5, routingKey);
    assertThat(mockSentSpan.operationName(), equalTo(RabbitMqTracingTags.SPAN_KIND_PRODUCER));
  }

  private void assertRabbitSpan(MockSpan mockSentSpan, String spanKind, int expectedTagsCount, String routingKey) {
    assertThat(mockSentSpan.tags(), notNullValue());
    assertThat(mockSentSpan.tags().size(), is(expectedTagsCount));
    assertSpanRabbitTags(mockSentSpan, spanKind, routingKey);
  }
}
