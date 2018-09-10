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

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitWithoutRabbitTemplateConfig {
  static final int PORT = 5672;

  @Bean
  public Queue queue() {
    return new Queue("myQueue", false);
  }

  @Bean
  public RabbitConnectionFactoryBean rabbitConnectionFactoryBean() throws Exception {
    RabbitConnectionFactoryBean rabbitConnectionFactoryBean = new RabbitConnectionFactoryBean();
    rabbitConnectionFactoryBean.setUsername("guest");
    rabbitConnectionFactoryBean.setPassword("guest");
    rabbitConnectionFactoryBean.setHost("localhost");
    rabbitConnectionFactoryBean.setVirtualHost("/");
    rabbitConnectionFactoryBean.setPort(PORT);
    rabbitConnectionFactoryBean.afterPropertiesSet();
    return rabbitConnectionFactoryBean;
  }

  @Bean
  public CachingConnectionFactory cachingConnectionFactory(
      RabbitConnectionFactoryBean rabbitConnectionFactoryBean) throws Exception {
    return new CachingConnectionFactory(rabbitConnectionFactoryBean.getObject());
  }

  @Bean
  public RabbitAdmin rabbitAdmin(Queue queue, CachingConnectionFactory cachingConnectionFactory) {
    final TopicExchange exchange = new TopicExchange("myExchange", true, false);

    final RabbitAdmin admin = new RabbitAdmin(cachingConnectionFactory);
    admin.declareQueue(queue);
    admin.declareExchange(exchange);
    admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("#"));

    return admin;
  }

  @Bean
  public SimpleMessageListenerContainer messageListenerContainer(
      CachingConnectionFactory cachingConnectionFactory, Queue queue) {
    SimpleMessageListenerContainer container =
        new SimpleMessageListenerContainer(cachingConnectionFactory);
    container.setQueues(queue);
    container.setMessageListener(new MessageListenerAdapter(new MessageListenerTest()));

    return container;
  }

  class MessageListenerTest {

    public void handleMessage(Object message) {}
  }
}
