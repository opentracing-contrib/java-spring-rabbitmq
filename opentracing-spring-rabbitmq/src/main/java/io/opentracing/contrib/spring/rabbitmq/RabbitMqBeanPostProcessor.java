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

import java.lang.reflect.Field;
import org.aopalliance.aop.Advice;

import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

/**
 * @author Gilles Robert
 * @author Sud Ramasamy
 */
class RabbitMqBeanPostProcessor implements BeanPostProcessor {

  private final RabbitMqReceiveTracingInterceptor rabbitMqReceiveTracingInterceptor;

  RabbitMqBeanPostProcessor(RabbitMqReceiveTracingInterceptor rabbitMqReceiveTracingInterceptor) {
    this.rabbitMqReceiveTracingInterceptor = rabbitMqReceiveTracingInterceptor;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    if (bean instanceof SimpleRabbitListenerContainerFactory) {
      SimpleRabbitListenerContainerFactory factory = (SimpleRabbitListenerContainerFactory) bean;
      registerTracingInterceptor(factory);
    } else if (bean instanceof SimpleMessageListenerContainer) {
      SimpleMessageListenerContainer container = (SimpleMessageListenerContainer) bean;
      registerTracingInterceptor(container);
    } else if (bean instanceof DirectRabbitListenerContainerFactory) {
      DirectRabbitListenerContainerFactory factory = (DirectRabbitListenerContainerFactory)bean;
      registerTracingInterceptor(factory);
    } else if (bean instanceof DirectMessageListenerContainer) {
      DirectMessageListenerContainer container = (DirectMessageListenerContainer)bean;
      registerTracingInterceptor(container);
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  private void registerTracingInterceptor(AbstractRabbitListenerContainerFactory factory) {
    Advice[] chain = factory.getAdviceChain();
    Advice[] adviceChainWithTracing = getAdviceChainOrAddInterceptorToChain(chain);
    factory.setAdviceChain(adviceChainWithTracing);
  }

  private void registerTracingInterceptor(AbstractMessageListenerContainer container) {
    Field adviceChainField =
        ReflectionUtils.findField(AbstractMessageListenerContainer.class, "adviceChain");
    ReflectionUtils.makeAccessible(adviceChainField);
    Advice[] chain = (Advice[]) ReflectionUtils.getField(adviceChainField, container);
    Advice[] adviceChainWithTracing = getAdviceChainOrAddInterceptorToChain(chain);
    container.setAdviceChain(adviceChainWithTracing);
  }

  private Advice[] getAdviceChainOrAddInterceptorToChain(Advice... existingAdviceChain) {
    if (existingAdviceChain == null) {
      return new Advice[] {rabbitMqReceiveTracingInterceptor};
    }

    for (Advice advice : existingAdviceChain) {
      if (advice instanceof RabbitMqReceiveTracingInterceptor) {
        return existingAdviceChain;
      }
    }

    Advice[] newChain = new Advice[existingAdviceChain.length + 1];
    System.arraycopy(existingAdviceChain, 0, newChain, 0, existingAdviceChain.length);
    newChain[existingAdviceChain.length] = rabbitMqReceiveTracingInterceptor;

    return newChain;
  }
}
