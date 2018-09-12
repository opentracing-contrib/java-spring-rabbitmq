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
package io.opentracing.contrib.spring.rabbitmq.customizing;

import io.opentracing.Span;
import io.opentracing.contrib.spring.rabbitmq.RabbitMqSpanDecorator;
import io.opentracing.contrib.spring.rabbitmq.RabbitMqTracingTags;

import org.springframework.amqp.core.MessageProperties;

/**
 * @author Ats Uiboupin
 */
public class CustomizedRabbitMqSpanDecorator extends RabbitMqSpanDecorator {
  public static final String OVERRIDEN_OPERATION_NAME_FOR_SENDING = RabbitMqTracingTags.SPAN_KIND_PRODUCER + ": overridden";
  public static final String OVERRIDEN_OPERATION_NAME_FOR_RECEIVING = RabbitMqTracingTags.SPAN_KIND_CONSUMER + ": overridden";

  @Override
  protected void onSend(MessageProperties messageProperties, String exchange, String routingKey, Span span) {
    super.onSend(messageProperties, exchange, routingKey, span);
    span.setOperationName(OVERRIDEN_OPERATION_NAME_FOR_SENDING);
  }

  @Override
  protected void onReceive(MessageProperties messageProperties, Span span) {
    super.onReceive(messageProperties, span);
    span.setOperationName(OVERRIDEN_OPERATION_NAME_FOR_RECEIVING);
  }
}
