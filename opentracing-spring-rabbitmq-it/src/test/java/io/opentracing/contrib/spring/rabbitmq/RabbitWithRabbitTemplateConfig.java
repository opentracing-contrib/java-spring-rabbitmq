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

import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Ats Uiboupin
 */
/**
 * Separated from {@link RabbitWithoutRabbitTemplateConfig} to allow creating tests without RabbitTemplate (defined in this conf)
 */
@Configuration
@Import(RabbitWithoutRabbitTemplateConfig.class)
public class RabbitWithRabbitTemplateConfig {

  static final long RABBIT_TEMPLATE_REPLY_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(1);

  @Bean
  public RabbitTemplate rabbitTemplate(RabbitConnectionFactoryBean rabbitConnectionFactoryBean) throws Exception {
    CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(rabbitConnectionFactoryBean.getObject());
    RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
    rabbitTemplate.setExchange("myExchange");
    return configureRabbitTemplate(rabbitTemplate);
  }

  static RabbitTemplate configureRabbitTemplate(RabbitTemplate rabbitTemplate) {
    SimpleMessageConverter messageConverter = createMessageConverter();
    rabbitTemplate.setMessageConverter(messageConverter);
    rabbitTemplate.setReplyTimeout(RABBIT_TEMPLATE_REPLY_TIMEOUT_MILLIS);
    return rabbitTemplate;
  }

  private static SimpleMessageConverter createMessageConverter() {
    SimpleMessageConverter messageConverter = new SimpleMessageConverter();
    messageConverter.setCreateMessageIds(true);
    return messageConverter;
  }
}
