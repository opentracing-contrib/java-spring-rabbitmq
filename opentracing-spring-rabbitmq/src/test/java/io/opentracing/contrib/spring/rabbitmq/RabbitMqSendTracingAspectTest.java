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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.opentracing.Span;
import io.opentracing.mock.MockTracer;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gilles Robert
 */
@Import(value = {MockTracingConfiguration.class, RabbitMqSpanDecoratorConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class RabbitMqSendTracingAspectTest {

  private static final String ROUTING_KEY = "io.opentracing.event.AnEvent";

  private RabbitMqSendTracingAspect aspect;
  @Autowired
  private MockTracer mockTracer;
  @Autowired
  private RabbitMqSpanDecorator spanDecorator;
  @Mock
  private ProceedingJoinPoint proceedingJoinPoint;
  @Mock
  private MessageConverter messageConverter;
  @Mock
  private RabbitTemplate rabbitTemplate;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    aspect = new RabbitMqSendTracingAspect(mockTracer, spanDecorator);
  }

  @After
  public void tearDown() {
    TestUtils.verifyNoMoreInteractionsWithMocks(this);
  }

  @Test
  public void testTraceRabbitSend_whenNoPropertiesHeaders() throws Throwable {
    // given
    String exchange = "opentracing.event.exchange";
    String routingKey = ROUTING_KEY;

    Span span = mockTracer.buildSpan("test").start();

    mockTracer.scopeManager().activate(span);

    TestMessage<String> myMessage = new TestMessage<>("");

    Object[] args = new Object[] {exchange, routingKey, myMessage};
    given(proceedingJoinPoint.getArgs()).willReturn(args);

    MessageProperties properties = new MessageProperties();
    properties.setReceivedExchange("exchange");
    properties.setReceivedRoutingKey("routingKey");
    properties.setMessageId("messageId");
    Message message = new Message("".getBytes(), properties);
    given(messageConverter.toMessage(any(Object.class), any(MessageProperties.class)))
        .willReturn(message);

    given(proceedingJoinPoint.getTarget()).willReturn(rabbitTemplate);

    given(rabbitTemplate.getMessageConverter()).willReturn(messageConverter);

    // when
    aspect.traceRabbitSend(proceedingJoinPoint, exchange, routingKey, myMessage);

    // then
    verify(proceedingJoinPoint).getArgs();
    verify(rabbitTemplate).getMessageConverter();
    verify(proceedingJoinPoint, times(2)).getTarget();
    verify(messageConverter).toMessage(any(Object.class), any(MessageProperties.class));
    verify(proceedingJoinPoint).proceed(args);
  }

  @Test
  public void testTraceRabbitSend_whenNoConversionIsNeeded() throws Throwable {
    // given
    String exchange = "opentracing.event.exchange";

    MessageProperties properties = new MessageProperties();
    Message message = new Message("".getBytes(), properties);
    Object[] args = new Object[] {exchange, ROUTING_KEY, message};
    given(proceedingJoinPoint.getArgs()).willReturn(args);

    given(proceedingJoinPoint.getTarget()).willReturn(rabbitTemplate);

    given(messageConverter.toMessage(any(Object.class), any(MessageProperties.class)))
        .willReturn(message);

    // when
    aspect.traceRabbitSend(proceedingJoinPoint, exchange, ROUTING_KEY, message);

    // then
    verify(rabbitTemplate).getMessageConverter();
    verify(proceedingJoinPoint, times(2)).getTarget();
    verify(proceedingJoinPoint).getArgs();
    verify(proceedingJoinPoint).proceed(args);
  }

  @Test(expected = RuntimeException.class)
  public void testTraceRabbitSend_whenException() throws Throwable {
    // given
    String exchange = "opentracing.event.exchange";

    MessageProperties properties = new MessageProperties();
    Message message = new Message("".getBytes(), properties);
    Object[] args = new Object[] {exchange, ROUTING_KEY, message};
    given(proceedingJoinPoint.getArgs()).willReturn(args);

    given(proceedingJoinPoint.getTarget()).willReturn(rabbitTemplate);

    given(messageConverter.toMessage(any(Object.class), any(MessageProperties.class)))
        .willReturn(message);

    given(proceedingJoinPoint.proceed(args)).willThrow(new RuntimeException());

    try {
      // when
      aspect.traceRabbitSend(proceedingJoinPoint, exchange, ROUTING_KEY, message);
    } catch (RuntimeException e) {
      // then
      verify(rabbitTemplate).getMessageConverter();
      verify(proceedingJoinPoint).getArgs();
      verify(proceedingJoinPoint, times(2)).getTarget();
      verify(proceedingJoinPoint).proceed(args);

      throw e;
    }
  }

  @Test
  public void testTraceRabbitSend_whenTargetIsNotRabbitTemplate() throws Throwable {
    // given
    String exchange = "opentracing.event.exchange";

    MessageProperties properties = new MessageProperties();
    Message message = new Message("".getBytes(), properties);
    Object[] args = new Object[] {exchange, ROUTING_KEY, message};
    AmqpTemplate notRabbitTemplate = Mockito.mock(AmqpTemplate.class);
    given(proceedingJoinPoint.getArgs()).willReturn(args);

    given(proceedingJoinPoint.getTarget()).willReturn(rabbitTemplate);

    given(messageConverter.toMessage(any(Object.class), any(MessageProperties.class)))
        .willReturn(message);

    given(proceedingJoinPoint.getTarget()).willReturn(notRabbitTemplate);

    // when
    aspect.traceRabbitSend(proceedingJoinPoint, exchange, ROUTING_KEY, message);
    // then
    verify(proceedingJoinPoint).getArgs();
    verify(proceedingJoinPoint).getTarget();
    verify(proceedingJoinPoint).proceed(args);
  }

  class TestMessage<T> {

    private T body;

    TestMessage(T body) {
      this.body = body;
    }

    T getBody() {
      return body;
    }

    void setBody(T body) {
      this.body = body;
    }
  }
}
