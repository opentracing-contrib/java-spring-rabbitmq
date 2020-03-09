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

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.aop.AfterAdvice;
import org.springframework.aop.BeforeAdvice;

/**
 * @author Gilles Robert
 */
@AllArgsConstructor
class RabbitMqReceiveTracingInterceptor implements MethodInterceptor, AfterAdvice, BeforeAdvice {

  private final Tracer tracer;
  private final RabbitMqSpanDecorator spanDecorator;

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    Message message = (Message) methodInvocation.getArguments()[1];
    MessageProperties messageProperties = message.getMessageProperties();

    Optional<Scope> child = RabbitMqTracingUtils.buildReceiveSpan(messageProperties, tracer);
    child.ifPresent(scope -> spanDecorator.onReceive(messageProperties, tracer.scopeManager().activeSpan()));

    // CHECKSTYLE:OFF
    try {
      return methodInvocation.proceed();
    } catch (Exception ex) {
      // CHECKSTYLE:ON
      child.ifPresent(scope -> spanDecorator.onError(ex, tracer.scopeManager().activeSpan()));
      throw ex;
    } finally {
      child.ifPresent(it -> {
        Optional.ofNullable(tracer.scopeManager().activeSpan())
            .ifPresent(Span::finish);
        it.close();
      });
    }
  }
}
