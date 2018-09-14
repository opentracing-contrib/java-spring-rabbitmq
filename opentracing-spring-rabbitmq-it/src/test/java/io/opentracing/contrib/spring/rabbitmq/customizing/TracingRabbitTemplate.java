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

import com.google.common.base.Throwables;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.rabbitmq.RabbitMqSendTracingHelper;
import io.opentracing.contrib.spring.rabbitmq.RabbitMqSpanDecorator;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;

public class TracingRabbitTemplate extends RabbitTemplate {
  private final Tracer tracer;
  private final RabbitMqSpanDecorator spanDecorator;

  public TracingRabbitTemplate(ConnectionFactory connectionFactory, Tracer tracer, RabbitMqSpanDecorator spanDecorator) {
    super(connectionFactory);
    this.tracer = tracer;
    this.spanDecorator = spanDecorator;
  }

  @Override
  public void convertAndSend(String exchange, String routingKey, Object message) throws AmqpException {
    try {
      createTracingHelper()
          .doWithTracingHeadersMessage(exchange, routingKey, message, (messageWithTracingHeaders) -> {
            super.convertAndSend(exchange, routingKey, messageWithTracingHeaders);
            return null;
          });
    } catch (Throwable throwable) {
      Throwables.propagateIfPossible(throwable, AmqpException.class);
    }
  }

  @Override
  public void send(String exchange, String routingKey, Message message, CorrelationData correlationData) throws AmqpException {
    // used when sending reply to AMQP requests that expected response message
    // or when not waiting for reply (for example sending out events).
    if (routingKey != null && routingKey.startsWith(Address.AMQ_RABBITMQ_REPLY_TO + ".")) {
      super.send(exchange, routingKey, message, correlationData);
      spanDecorator.onSendReply(message.getMessageProperties(), exchange, routingKey, tracer.activeSpan());
      return; // don't create new span for response messages
    }
    try {
      createTracingHelper()
          .doWithTracingHeadersMessage(exchange, routingKey, message, (messageWithTracingHeaders) -> {
            super.send(exchange, routingKey, messageWithTracingHeaders, correlationData);
            return null;
          });
    } catch (Throwable throwable) {
      Throwables.propagateIfPossible(throwable, AmqpException.class);
    }
  }

  @Override
  protected Message doSendAndReceiveWithTemporary(String exchange, String routingKey, Message message, CorrelationData correlationData) {
    try {
      return createTracingHelper()
          .nullResponseMeansTimeout(this)
          .doWithTracingHeadersMessage(exchange, routingKey, message,
              (messageWithTracingHeaders) -> super.doSendAndReceiveWithTemporary(exchange, routingKey, messageWithTracingHeaders, correlationData));
    } catch (Throwable throwable) {
      throw Throwables.propagate(throwable);
    }
  }

  @Override
  protected Message doSendAndReceiveWithFixed(String exchange, String routingKey, Message message, CorrelationData correlationData) {
    try {
      return createTracingHelper()
          .doWithTracingHeadersMessage(exchange, routingKey, message, (messageWithTracingHeaders) -> {
            // returns null in case of timeout
            return super.doSendAndReceiveWithFixed(exchange, routingKey, messageWithTracingHeaders, correlationData);
          });
    } catch (Throwable throwable) {
      throw Throwables.propagate(throwable);
    }
  }

  private RabbitMqSendTracingHelper createTracingHelper() {
    return new RabbitMqSendTracingHelper(tracer, this.getMessageConverter(), spanDecorator);
  }

}
