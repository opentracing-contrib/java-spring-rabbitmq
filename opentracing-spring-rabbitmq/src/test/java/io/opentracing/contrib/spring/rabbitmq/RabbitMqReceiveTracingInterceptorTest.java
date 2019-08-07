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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicLong;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gilles Robert
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {MockTracingConfiguration.class, RabbitMqSpanDecoratorConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class RabbitMqReceiveTracingInterceptorTest {

  @Autowired
  private MockTracer mockTracer;
  @Spy
  private RabbitMqSpanDecorator spanDecorator;

  @Before
  public void setup() throws Exception {
    mockTracer.reset();
    resetSpanIdCounter();
  }

  private void resetSpanIdCounter() throws NoSuchFieldException, IllegalAccessException {
    Field spanIdCounter = MockSpan.class.getDeclaredField("nextId");
    spanIdCounter.setAccessible(true);
    ((AtomicLong) spanIdCounter.get(null)).set(0L);
  }

  @Test
  public void testInvoke_whenContextAndActiveSpan() throws Throwable {
    // given
    mockTracer.buildSpan("parent").startActive(false);
    RabbitMqReceiveTracingInterceptor interceptor = new RabbitMqReceiveTracingInterceptor(mockTracer, spanDecorator);
    MethodInvocation methodInvocation = new TestMethodInvocationWithContext();

    // when
    interceptor.invoke(methodInvocation);

    // then
    assertTraceAndSpanId("1", "3");
  }

  @Test
  public void testInvoke_whenContextAndNoActiveSpan() throws Throwable {
    // given
    RabbitMqReceiveTracingInterceptor interceptor = new RabbitMqReceiveTracingInterceptor(mockTracer, spanDecorator);
    MethodInvocation methodInvocation = new TestMethodInvocation();

    // when
    interceptor.invoke(methodInvocation);

    // then
    assertTraceAndSpanId("1", "2");
  }

  @Test
  public void testInvoke_whenNoContext() throws Throwable {
    // given
    RabbitMqReceiveTracingInterceptor interceptor = new RabbitMqReceiveTracingInterceptor(mockTracer, spanDecorator);
    MethodInvocation methodInvocation = new TestMethodInvocation();

    // when
    interceptor.invoke(methodInvocation);

    // then
    assertTraceAndSpanId("1", "2");
  }

  @Test(expected = RuntimeException.class)
  public void testInvoke_whenException() throws Throwable {
    // given
    RabbitMqReceiveTracingInterceptor interceptor = new RabbitMqReceiveTracingInterceptor(mockTracer, spanDecorator);
    MethodInvocation methodInvocation = new TestExceptionMethodInvocation();

    // when
    interceptor.invoke(methodInvocation);

    // then
  }

  @Test(expected = RuntimeException.class)
  public void testInvoke_whenExceptionAndChildPresent() throws Throwable {
    // given
    RabbitMqReceiveTracingInterceptor interceptor = new RabbitMqReceiveTracingInterceptor(mockTracer, spanDecorator);
    MethodInvocation methodInvocation = new TestExceptionMethodInvocationWithContext();

    // when
    interceptor.invoke(methodInvocation);

    // then
  }

  private void assertTraceAndSpanId(String traceId, String spanId) {
    ArgumentCaptor<Span> span = ArgumentCaptor.forClass(Span.class);
    verify(spanDecorator).onReceive(any(MessageProperties.class), span.capture());
    assertThat(span.getValue().context().toTraceId()).isEqualTo(traceId);
    assertThat(span.getValue().context().toSpanId()).isEqualTo(spanId);
  }

  private Message getMessage() {
    final MessageProperties messageProperties = new MessageProperties();
    messageProperties.setReceivedExchange("exchange");
    messageProperties.setReceivedRoutingKey("routingKey");
    messageProperties.setMessageId("messageId");
    return new Message("".getBytes(Charset.defaultCharset()), messageProperties);
  }

  private Message getMessageWithContext() {
    Message message = getMessage();
    MessageProperties messageProperties = message.getMessageProperties();
    messageProperties.setHeader("traceid", 1L);
    messageProperties.setHeader("spanid", 1L);
    return new Message("".getBytes(Charset.defaultCharset()), messageProperties);
  }

  private class TestMethodInvocation implements MethodInvocation {

    @Override
    public Method getMethod() {
      return null;
    }

    @Override
    public Object[] getArguments() {
      Message message = getMessage();
      return new Object[] {null, message};
    }

    @Override
    public Object proceed() {
      return null;
    }

    @Override
    public Object getThis() {
      return null;
    }

    @Override
    public AccessibleObject getStaticPart() {
      return null;
    }
  }

  private class TestMethodInvocationWithContext implements MethodInvocation {

    @Override
    public Method getMethod() {
      return null;
    }

    @Override
    public Object[] getArguments() {
      Message message = getMessageWithContext();
      return new Object[] {null, message};
    }

    @Override
    public Object proceed() {
      return null;
    }

    @Override
    public Object getThis() {
      return null;
    }

    @Override
    public AccessibleObject getStaticPart() {
      return null;
    }
  }

  private class TestExceptionMethodInvocation implements MethodInvocation {

    @Override
    public Method getMethod() {
      return null;
    }

    @Override
    public Object[] getArguments() {
      Message message = getMessage();
      return new Object[] {null, message};
    }

    @Override
    public Object proceed() {
      throw new RuntimeException();
    }

    @Override
    public Object getThis() {
      return null;
    }

    @Override
    public AccessibleObject getStaticPart() {
      return null;
    }
  }

  private class TestExceptionMethodInvocationWithContext implements MethodInvocation {

    @Override
    public Method getMethod() {
      return null;
    }

    @Override
    public Object[] getArguments() {
      Message message = getMessageWithContext();
      return new Object[] {null, message};
    }

    @Override
    public Object proceed() {
      throw new RuntimeException();
    }

    @Override
    public Object getThis() {
      return null;
    }

    @Override
    public AccessibleObject getStaticPart() {
      return null;
    }
  }
}
