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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import io.opentracing.contrib.spring.rabbitmq.testrule.TestRabbitServerResource;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;

import java.util.Collection;
import java.util.List;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
// Needed to destroy rabbit message listeners (created by RabbitWithoutRabbitTemplateConfig.messageListenerContainer)
// to avoid race condition errors related to listeners from previous test applicationContext grabbing messages
// of next test that results in rabbit-receive span being added to MockTracer of previous test
// and hence next test never sees that span when checking finished spans.
@DirtiesContext
public abstract class BaseRabbitMqTracingItTest {
  @ClassRule // could remove this annotation to reuse locally running RabbitMq server to speed up tests
  public static TestRabbitServerResource rabbitServer = new TestRabbitServerResource();

  @Autowired protected MockTracer tracer;

  @Before
  public void setup() {
    tracer.reset();
  }


  protected void assertConsumerAndProducerSpans(long parentSpanId) {
    List<MockSpan> spans = awaitFinishedSpans();
    assertEquals(2, spans.size());
    assertRabbitProducerSpan(spans.get(0), parentSpanId);

    assertRabbitConsumerSpan(spans.get(1));
    assertSameTraceId(spans);
  }

  private void assertRabbitConsumerSpan(MockSpan mockReceivedSpan) {
    String expectedConsumerQueueName = "myQueue";
    assertRabbitSpan(mockReceivedSpan, RabbitMqTracingTags.SPAN_KIND_CONSUMER, 6);
    assertThat(mockReceivedSpan.operationName(), equalTo(RabbitMqTracingTags.SPAN_KIND_CONSUMER));
    assertThat(mockReceivedSpan.tags().get("consumerqueue"), equalTo(expectedConsumerQueueName));
    assertThat(mockReceivedSpan.generatedErrors().size(), is(0));
  }

  private void assertRabbitProducerSpan(MockSpan mockSentSpan, long expectedParentSpanId) {
    assertThat(mockSentSpan.parentId(), equalTo(expectedParentSpanId));
    assertRabbitSpan(mockSentSpan, RabbitMqTracingTags.SPAN_KIND_PRODUCER, 5);
    assertThat(mockSentSpan.operationName(), equalTo(RabbitMqTracingTags.SPAN_KIND_PRODUCER));
  }

  private void assertRabbitSpan(MockSpan mockSentSpan, String spanKind, int expectedTagsCount) {
    assertThat(mockSentSpan.tags(), notNullValue());
    assertThat(mockSentSpan.tags().size(), is(expectedTagsCount));
    assertSpanRabbitTags(mockSentSpan, spanKind);
  }

  protected void assertSpanRabbitTags(MockSpan mockSentSpan, String spanKind) {
    assertThat(mockSentSpan.tags().get("messageid"), notNullValue());
    assertThat(
        mockSentSpan.tags().get("component"),
        equalTo(RabbitMqTracingTags.RABBITMQ.getKey()));
    assertThat(mockSentSpan.tags().get("exchange"), equalTo("myExchange"));
    assertThat(
        mockSentSpan.tags().get("span.kind"),
        equalTo(spanKind));
    assertThat(mockSentSpan.tags().get("routingkey"), equalTo("#"));
  }

  private void assertSameTraceId(Collection<MockSpan> spans) {
    if (!spans.isEmpty()) {
      final long traceId = spans.iterator().next().context().traceId();
      for (MockSpan span : spans) {
        assertEquals(traceId, span.context().traceId());
      }
    }
  }

  protected List<MockSpan> awaitFinishedSpans() {
    await()
        .timeout(Duration.TWO_SECONDS)
        .until(
            () -> {
              List<MockSpan> mockSpans = tracer.finishedSpans();
              return (mockSpans.size() == 2);
            });

    return tracer.finishedSpans();
  }

  protected void checkNoSpans() {
    try {
      List<MockSpan> spans = awaitFinishedSpans();
      Assert.fail("Expected not to receive trace spans when autoconfiguration isn't enabled, but got: " + spans);
    } catch (ConditionTimeoutException e) {
      assertThat(tracer.finishedSpans().size(), equalTo(0));
    }
  }

}
