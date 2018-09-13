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

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import java.lang.reflect.Field;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.util.ReflectionUtils;

@RequiredArgsConstructor
class RabbitMqSendTracingHelper {
  private static final Field FIELD_REPLY_TIMEOUT = ReflectionUtils.findField(RabbitTemplate.class, "replyTimeout");
  static {
    FIELD_REPLY_TIMEOUT.setAccessible(true);
  }

  private final Tracer tracer;
  private final MessageConverter messageConverter;
  private final RabbitMqSpanDecorator spanDecorator;
  private Scope scope;
  private boolean nullResponseMeansError;
  private RabbitTemplate rabbitTemplate;

  RabbitMqSendTracingHelper nullResponseMeansTimeout(RabbitTemplate rabbitTemplate) {
    this.nullResponseMeansError = true;
    this.rabbitTemplate = rabbitTemplate;
    return this;
  }

  Object doWithTracingHeadersMessage(String exchange, String routingKey, Object message, ProceedFunction proceedCallback)
      throws Throwable {
    if (routingKey != null && routingKey.startsWith(Address.AMQ_RABBITMQ_REPLY_TO + ".")) {
      // don't create new span for response messages when sending reply to AMQP requests that expected response message
      Message convertedMessage = convertMessageIfNecessary(message);
      return proceedCallback.apply(convertedMessage);
    }
    Message messageWithTracingHeaders = doBefore(exchange, routingKey, message);
    try {
      Object resp = proceedCallback.apply(messageWithTracingHeaders);
      if (resp == null && nullResponseMeansError) {
        Span span = scope.span();
        spanDecorator.onError(null, span);
        long replyTimeout = (long) ReflectionUtils.getField(FIELD_REPLY_TIMEOUT, rabbitTemplate);
        span.log("Timeout: AMQP request message handler hasn't sent reply AMQP message in " + replyTimeout + "ms");
      }
      return resp;
    } catch (AmqpException ex) {
      spanDecorator.onError(ex, scope.span());
      throw ex;
    } finally {
      scope.close();
    }
  }

  private Message doBefore(String exchange, String routingKey, Object message) {
    Message convertedMessage = convertMessageIfNecessary(message);

    final MessageProperties messageProperties = convertedMessage.getMessageProperties();

    // Add tracing header to outgoing AMQP message
    // so that new spans created on the AMQP message consumer side could be associated with span of current trace
    scope = RabbitMqTracingUtils.buildSendSpan(tracer, messageProperties);
    tracer.inject(
        scope.span().context(),
        Format.Builtin.TEXT_MAP,
        new RabbitMqInjectAdapter(messageProperties));

    // Add AMQP related tags to tracing span
    spanDecorator.onSend(messageProperties, exchange, routingKey, scope.span());

    return convertedMessage;
  }

  private Message convertMessageIfNecessary(final Object object) {
    if (object instanceof Message) {
      return (Message) object;
    }

    return messageConverter.toMessage(object, new MessageProperties());
  }

  public interface ProceedFunction {
    Object apply(Message t) throws Throwable;
  }

}
