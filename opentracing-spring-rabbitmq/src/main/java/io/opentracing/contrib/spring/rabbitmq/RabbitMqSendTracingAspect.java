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

import io.opentracing.Tracer;

import lombok.AllArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * @author Gilles Robert
 */
@Aspect
@AllArgsConstructor
class RabbitMqSendTracingAspect {

  private final Tracer tracer;
  private final MessageConverter messageConverter;
  private final RabbitMqSpanDecorator spanDecorator;

  // CHECKSTYLE:OFF

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#convertAndSend(String, String, Object)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.convertAndSend(..)) && args(exchange,"
      + "routingKey, message)", argNames = "pjp,exchange,routingKey,message"
  )
  public Object traceRabbitSend(
      ProceedingJoinPoint pjp, String exchange, String routingKey, Object message)
      throws Throwable {
    return createTracingHelper()
        .doWithTracingHeadersMessage(exchange, routingKey, message, (convertedMessage) ->
            proceedReplacingMessage(pjp, convertedMessage, 2));
  }
  // CHECKSTYLE:ON

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // START: intercept public methods that eventually delegate to RabbitTemplate.doSendAndReceive                      //
  // that can't be intercepted with AspectJ as it is protected

  /**
   * @see org.springframework.amqp.rabbit.core.RabbitTemplate#sendAndReceive(String, String, Message, CorrelationData)
   */
  @Around(value = "execution(* org.springframework.amqp.rabbit.core.RabbitTemplate.sendAndReceive(..)) && args(exchange,"
      + "routingKey, message, correlationData)", argNames = "pjp,exchange,routingKey,message,correlationData"
  )
  public Object traceRabbitSendAndReceive(
      ProceedingJoinPoint pjp, String exchange, String routingKey, Message message, CorrelationData correlationData)
      throws Throwable {
    return createTracingHelper()
        .doWithTracingHeadersMessage(exchange, routingKey, message, (convertedMessage) ->
            proceedReplacingMessage(pjp, convertedMessage, 2));
  }

  // END: intercept public methods that eventually delegate to RabbitTemplate.doSendAndReceive                        //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private RabbitMqSendTracingHelper createTracingHelper() {
    return new RabbitMqSendTracingHelper(tracer, messageConverter, spanDecorator);
  }

  private Object proceedReplacingMessage(ProceedingJoinPoint pjp, Message convertedMessage, int messageArgumentIndex)
      throws Throwable {
    final Object[] args = pjp.getArgs();
    args[messageArgumentIndex] = convertedMessage;
    return pjp.proceed(args);
  }

}
