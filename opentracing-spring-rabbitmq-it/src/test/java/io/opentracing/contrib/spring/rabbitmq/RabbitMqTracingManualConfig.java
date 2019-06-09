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

import io.opentracing.Tracer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.Assert;

/**
 * @author Ats Uiboupin
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class RabbitMqTracingManualConfig {

  @Autowired
  @Lazy
  private Tracer tracer;

  @Bean
  public RabbitMqSendTracingAspect rabbitMqSendTracingAspect(RabbitTemplate rabbitTemplate,
                                                             RabbitMqSpanDecorator spanDecorator) {
    Assert.notNull(rabbitTemplate.getMessageConverter(), "RabbitTemplate has no message converter configured");

    return new RabbitMqSendTracingAspect(tracer, rabbitTemplate.getExchange(), rabbitTemplate.getRoutingKey(),
        rabbitTemplate.getMessageConverter(), spanDecorator);
  }

  @Bean
  public RabbitMqReceiveTracingInterceptor rabbitMqReceiveTracingInterceptor(RabbitMqSpanDecorator spanDecorator) {
    return new RabbitMqReceiveTracingInterceptor(tracer, spanDecorator);
  }

  @Bean
  public RabbitMqBeanPostProcessor rabbitMqBeanPostProcessor(RabbitMqReceiveTracingInterceptor interceptor) {
    return new RabbitMqBeanPostProcessor(interceptor);
  }

  @Bean
  public RabbitMqSpanDecorator rabbitMqSpanDecorator() {
    return new RabbitMqSpanDecorator();
  }
}
