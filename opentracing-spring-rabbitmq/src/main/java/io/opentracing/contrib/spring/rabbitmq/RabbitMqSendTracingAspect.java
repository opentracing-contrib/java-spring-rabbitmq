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
package io.opentracing.contrib.spring.rabbitmq;

import io.opentracing.Tracer;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * @author Gilles Robert
 */
@Aspect
@AllArgsConstructor
class RabbitMqSendTracingAspect {

  private final Tracer tracer;
  private final RabbitMqSpanDecorator spanDecorator;

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#send(Message)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.send(..)) && args(message)",
      argNames = "pjp,message")
  public Object traceRabbitSend(ProceedingJoinPoint pjp, Object message) throws Throwable {
    return tagAndProceed(pjp, null, null, message, 0, false);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#send(String, Message)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.send(..)) && args(routingKey, message)",
      argNames = "pjp,routingKey,message")
  public Object traceRabbitSend(ProceedingJoinPoint pjp, String routingKey, Object message) throws Throwable {
    return tagAndProceed(pjp, null, routingKey, message, 1, false);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#send(String, String, Message)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.send(..)) && args(exchange, " +
                  "routingKey, message)", argNames = "pjp,exchange, routingKey, message")
  public Object traceRabbitSend(ProceedingJoinPoint pjp, String exchange, String routingKey, Object message)
      throws Throwable {
    return tagAndProceed(pjp, exchange, routingKey, message, 2, false);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#convertAndSend(Object)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.convertAndSend(..)) " +
                  "&& args(message)", argNames = "pjp,message")
  public Object traceRabbitConvertAndSend(ProceedingJoinPoint pjp, Object message) throws Throwable {
    return tagAndProceed(pjp, null, null, message, 0, false);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#convertAndSend(String, Object)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.convertAndSend(..)) " +
      "&& args(routingKey, message)", argNames = "pjp,routingKey,message")
  public Object traceRabbitConvertAndSend(
      ProceedingJoinPoint pjp, String routingKey, Object message)
      throws Throwable {
    return tagAndProceed(pjp, null, routingKey, message, 1, false);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#convertAndSend(String, String, Object)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.convertAndSend(..)) && args(exchange,"
      + "routingKey, message)", argNames = "pjp,exchange,routingKey,message")
  public Object traceRabbitConvertAndSend(
      ProceedingJoinPoint pjp, String exchange, String routingKey, Object message)
      throws Throwable {
    return tagAndProceed(pjp, exchange, routingKey, message, 2, false);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#convertAndSend(Object, MessagePostProcessor)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.convertAndSend(..))" +
                  " && args(message, messagePostProcessor)", argNames = "pjp,message,messagePostProcessor")
  public Object traceRabbitConvertAndSend(ProceedingJoinPoint pjp, Object message,
                                          MessagePostProcessor messagePostProcessor) throws Throwable {
    return tagAndProceed(pjp, null, null, message, 0, false);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#convertAndSend(String, Object, MessagePostProcessor)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.convertAndSend(..))" +
                  " && args(routingKey, message, messagePostProcessor)",
      argNames = "pjp,routingKey,message,messagePostProcessor")
  public Object traceRabbitConvertAndSend(ProceedingJoinPoint pjp, String routingKey, Object message,
                                          MessagePostProcessor messagePostProcessor) throws Throwable {
    return tagAndProceed(pjp, null, routingKey, message, 1, false);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#convertAndSend(String, String, Object, MessagePostProcessor)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.convertAndSend(..))" +
                  " && args(exchange, routingKey, message, messagePostProcessor)",
      argNames = "pjp,exchange,routingKey,message,messagePostProcessor")
  public Object traceRabbitConvertAndSend(ProceedingJoinPoint pjp,String exchange, String routingKey, Object message,
                                          MessagePostProcessor messagePostProcessor) throws Throwable {
    return tagAndProceed(pjp, exchange, routingKey, message, 2, false);
  }

  /**
   * @see RabbitTemplate#convertAndSend(String, Object, MessagePostProcessor, CorrelationData)
   */
  @Around(value = "execution(* org.springframework.amqp.rabbit.core.RabbitTemplate.convertAndSend(..)) " +
                  "&& args(routingKey, message, messagePostProcessor, correlationData)",
      argNames = "pjp,routingKey,message,messagePostProcessor,correlationData")
  public Object traceRabbitConvertAndSend(ProceedingJoinPoint pjp, String routingKey, Object message,
                                          MessagePostProcessor messagePostProcessor, CorrelationData correlationData)
      throws Throwable {
    return tagAndProceed(pjp, null, routingKey, message, 1, false);
  }

  /**
   * @see RabbitTemplate#convertAndSend(String, String, Object, CorrelationData)
   */
  @Around(value = "execution(* org.springframework.amqp.rabbit.core.RabbitTemplate.convertAndSend(..)) " +
          "&& args(exchange, routingKey, message, correlationData)",
          argNames = "pjp,exchange,routingKey,message,correlationData")
  public Object traceRabbitConvertAndSend(ProceedingJoinPoint pjp, String exchange, String routingKey, Object message,
                                          CorrelationData correlationData)
          throws Throwable {
    return tagAndProceed(pjp, exchange, routingKey, message, 2, false);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#sendAndReceive(Message)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.sendAndReceive(..))" +
                  " && args(message)", argNames = "pjp,message")
  public Object traceRabbitSendAndReceive(ProceedingJoinPoint pjp, Object message) throws Throwable {
    return tagAndProceed(pjp, null, null, message, 0, true);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#sendAndReceive(String, Message)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.sendAndReceive(..))" +
                  " && args(routingKey, message)", argNames = "pjp,routingKey,message")
  public Object traceRabbitSendAndReceive(ProceedingJoinPoint pjp, String routingKey, Object message) throws Throwable {
    return tagAndProceed(pjp, null, routingKey, message, 1, true);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#sendAndReceive(String, String, Message)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.sendAndReceive(..))" +
                  " && args(exchange, routingKey, message)", argNames = "pjp,exchange,routingKey,message")
  public Object traceRabbitSendAndReceive(ProceedingJoinPoint pjp, String exchange, String routingKey, Object message)
      throws Throwable {
    return tagAndProceed(pjp, exchange, routingKey, message, 2, true);
  }

  // Intercept public methods that eventually delegate to RabbitTemplate.doSendAndReceive
  // that can't be intercepted with AspectJ as it is protected.
  /**
   * @see RabbitTemplate#sendAndReceive(String, String, Message, CorrelationData)
   */
  @Around(value = "execution(* org.springframework.amqp.rabbit.core.RabbitTemplate.sendAndReceive(..)) && args(exchange,"
      + "routingKey, message, correlationData)", argNames = "pjp,exchange,routingKey,message,correlationData")
  public Object traceRabbitSendAndReceive(
      ProceedingJoinPoint pjp, String exchange, String routingKey, Message message, CorrelationData correlationData)
      throws Throwable {
    return tagAndProceed(pjp, exchange, routingKey, message, 2, true);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#convertSendAndReceive(Object)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.convertSendAndReceive(..))" +
                  " && args(message)", argNames = "pjp,message")
  public Object traceRabbitConvertSendAndReceive(ProceedingJoinPoint pjp, Object message)
      throws Throwable {
    return tagAndProceed(pjp, null, null, message, 0, true);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#convertSendAndReceive(String, Object)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.convertSendAndReceive(..))" +
                  " && args(routingKey, message)", argNames = "pjp,routingKey,message")
  public Object traceRabbitConvertSendAndReceive(ProceedingJoinPoint pjp, String routingKey, Object message)
      throws Throwable {
    return tagAndProceed(pjp, null, routingKey, message, 1, true);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#convertSendAndReceive(String, String, Object)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.convertSendAndReceive(..))" +
                  " && args(exchange, routingKey, message)", argNames = "pjp,exchange,routingKey,message")
  public Object traceRabbitConvertSendAndReceive(ProceedingJoinPoint pjp, String exchange, String routingKey,
                                                 Object message)
      throws Throwable {
    return tagAndProceed(pjp, exchange, routingKey, message, 2, true);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#convertSendAndReceive(Object, MessagePostProcessor)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.convertSendAndReceive(..))" +
                  " && args(message, messagePostProcessor)", argNames = "pjp,message,messagePostProcessor")
  public Object traceRabbitConvertSendAndReceive(ProceedingJoinPoint pjp, Object message,
                                                 MessagePostProcessor messagePostProcessor)
      throws Throwable {
    return tagAndProceed(pjp, null, null, message, 0, true);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#convertSendAndReceive(String, Object, MessagePostProcessor)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.convertSendAndReceive(..))" +
                  " && args(routingKey, message, messagePostProcessor)",
      argNames = "pjp,routingKey,message,messagePostProcessor")
  public Object traceRabbitConvertSendAndReceive(ProceedingJoinPoint pjp, String routingKey, Object message,
                                                 MessagePostProcessor messagePostProcessor)
      throws Throwable {
    return tagAndProceed(pjp, null, routingKey, message, 1, true);
  }

  /**
   * @see org.springframework.amqp.core.AmqpTemplate#convertSendAndReceive(String, String, Object, MessagePostProcessor)
   */
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.convertSendAndReceive(..))" +
                  " && args(exchange, routingKey, message, messagePostProcessor)",
      argNames = "pjp,exchange,routingKey,message,messagePostProcessor")
  public Object traceRabbitConvertSendAndReceive(ProceedingJoinPoint pjp, String exchange, String routingKey,
                                                 Object message, MessagePostProcessor messagePostProcessor)
      throws Throwable {
    return tagAndProceed(pjp, exchange, routingKey, message, 2, true);
  }

  /**
   * @see RabbitTemplate#convertSendAndReceive(String, String, Object, MessagePostProcessor, CorrelationData)
   */
  @Around(value = "execution(* org.springframework.amqp.rabbit.core.RabbitTemplate.convertSendAndReceive(..)) " +
                  "&& args(exchange, routingKey, message, messagePostProcessor, correlationData)",
      argNames = "pjp,exchange,routingKey,message,messagePostProcessor,correlationData")
  public Object traceRabbitConvertSendAndReceive(
      ProceedingJoinPoint pjp, String exchange, String routingKey, Object message,
      MessagePostProcessor messagePostProcessor, CorrelationData correlationData)
      throws Throwable {
    return tagAndProceed(pjp, exchange, routingKey, message, 2, true);
  }

  private Object tagAndProceed(ProceedingJoinPoint pjp, String exchange, String routingKey,
                               Object message, int messageIndex, boolean nullResponseMeansTimeout)
      throws Throwable {
    if (pjp.getTarget() instanceof RabbitTemplate) {
      RabbitTemplate rabbitTemplate = (RabbitTemplate) pjp.getTarget();
      MessageConverter converter = rabbitTemplate.getMessageConverter();
      String exactlyRoutingKey = Optional.ofNullable(routingKey).orElseGet(rabbitTemplate::getRoutingKey);
      String exactlyExchange = Optional.ofNullable(exchange).orElseGet(rabbitTemplate::getExchange);

      RabbitMqSendTracingHelper helper = new RabbitMqSendTracingHelper(tracer, converter, spanDecorator);
      if (nullResponseMeansTimeout) {
        helper.nullResponseMeansTimeout(rabbitTemplate);
      }
      return helper
          .doWithTracingHeadersMessage(exactlyExchange, exactlyRoutingKey, message,
              (convertedMessage) -> proceedReplacingMessage(pjp, convertedMessage, messageIndex));
    }
    return pjp.proceed(pjp.getArgs());
  }

  private Object proceedReplacingMessage(ProceedingJoinPoint pjp, Message convertedMessage, int messageArgumentIndex)
      throws Throwable {
    final Object[] args = pjp.getArgs();
    args[messageArgumentIndex] = convertedMessage;
    return pjp.proceed(args);
  }

}
