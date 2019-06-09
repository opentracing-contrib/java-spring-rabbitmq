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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTemplateProviderConfig {

  @Bean
  @ConditionalOnMissingBean(RabbitTemplateProvider.class)
  public RabbitTemplateProvider rabbitTemplateProvider(RabbitTemplate rabbitTemplate) {
    return new RabbitTemplateProvider(rabbitTemplate);
  }

  /**
   * In real-world separate {@link RabbitTemplate} instance might be created for example for each queue
   */
  @AllArgsConstructor
  public static class RabbitTemplateProvider {

    @Getter
    private RabbitTemplate rabbitTemplate;
  }
}
