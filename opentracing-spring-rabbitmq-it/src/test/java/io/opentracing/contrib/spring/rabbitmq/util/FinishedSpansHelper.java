/**
 * Copyright 2017-2020 The OpenTracing Authors
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
package io.opentracing.contrib.spring.rabbitmq.util;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import io.opentracing.contrib.spring.rabbitmq.RabbitMqTracingTags;
import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * @author Ats Uiboupin
 */
@Data
public class FinishedSpansHelper {

  private final List<MockSpan> allSpans;
  private MockSpan sendSpan;
  private MockSpan receiveSpan;

  public FinishedSpansHelper(List<MockSpan> spans) {
    this.allSpans = spans;
    assertThat("Didn't more than 2 spans", spans.size(), lessThanOrEqualTo(2));
    for (MockSpan span : spans) {
      Object spanKind = getSpanKind(span.tags());
      if (RabbitMqTracingTags.SPAN_KIND_PRODUCER.equals(spanKind)) {
        assertThat("Didn't expect 2 producer spans", sendSpan, nullValue());
        sendSpan = span;
      } else if (RabbitMqTracingTags.SPAN_KIND_CONSUMER.equals(spanKind)) {
        assertThat("Didn't expect 2 consumer spans", receiveSpan, nullValue());
        receiveSpan = span;
      } else {
        fail("TODO handle unknown span: " + span);
      }
    }
  }

  private String getSpanKind(Map<String, Object> tags) {
    return tags
        .entrySet()
        .stream()
        .filter(entry -> Tags.SPAN_KIND.getKey().equals(entry.getKey()))
        .findFirst()
        .map(Map.Entry::getValue)
        .map(Object::toString)
        .orElse(null);
  }

  public void assertSameTraceId() {
    if (!allSpans.isEmpty()) {
      final long traceId = allSpans.iterator().next().context().traceId();
      for (MockSpan span : allSpans) {
        assertEquals(traceId, span.context().traceId());
      }
    }
  }
}
