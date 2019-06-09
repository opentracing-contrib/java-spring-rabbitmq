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
package io.opentracing.contrib.spring.rabbitmq.customizing;

import io.opentracing.Span;
import io.opentracing.contrib.spring.rabbitmq.RabbitMqSpanDecorator;
import io.opentracing.contrib.spring.rabbitmq.RabbitMqTracingTags;
import io.opentracing.contrib.spring.rabbitmq.RabbitWithoutRabbitTemplateConfig.TestMessageListener;
import io.opentracing.tag.Tags;

import org.springframework.amqp.core.MessageProperties;

/**
 * @author Ats Uiboupin
 * @author Gilles Robert
 */
public class CustomizedRabbitMqSpanDecorator extends RabbitMqSpanDecorator {

  private static final String OVERRIDDEN = "overridden";
  private static final String SEPARATOR = ": ";

  public static final String OVERRIDDEN_OPERATION_NAME_FOR_SENDING =
      String.join(SEPARATOR,RabbitMqTracingTags.SPAN_KIND_PRODUCER, OVERRIDDEN);
  public static final String OVERRIDDEN_OPERATION_NAME_FOR_RECEIVING =
      String.join(SEPARATOR,RabbitMqTracingTags.SPAN_KIND_CONSUMER, OVERRIDDEN);

  @Override
  public void onSend(MessageProperties messageProperties, String exchange, String routingKey, Span span) {
    super.onSend(messageProperties, exchange, routingKey, span);
    span.setOperationName(OVERRIDDEN_OPERATION_NAME_FOR_SENDING);
  }

  @Override
  public void onReceive(MessageProperties messageProperties, Span span) {
    super.onReceive(messageProperties, span);
    span.setOperationName(OVERRIDDEN_OPERATION_NAME_FOR_RECEIVING);
  }

  @Override
  public void onSendReply(MessageProperties replyMessageProperties, String replyExchange,
                          String replyRoutingKey, Span span) {
    String errorMessageToAdd = (String) replyMessageProperties
        .getHeaders()
        .get(TestMessageListener.HEADER_CUSTOM_RESPONSE_ERROR_MARKER_HEADER);

    if (errorMessageToAdd != null) {
      Tags.ERROR.set(span, true);
    }
  }
}
