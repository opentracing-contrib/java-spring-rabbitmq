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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.rabbitmq.client.Channel;
import io.opentracing.contrib.spring.rabbitmq.RabbitTemplateProviderConfig.RabbitTemplateProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportAutoConfiguration(RabbitTemplateProviderConfig.class)
public class RabbitWithoutRabbitTemplateConfig {
  public static final int PORT = 5672;

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
      TestMessageListener messageListener,
      CachingConnectionFactory cachingConnectionFactory, Queue queue) {
    SimpleMessageListenerContainer container =
        new SimpleMessageListenerContainer(cachingConnectionFactory);
    container.setQueues(queue);
    container.setMessageListener(messageListener);

    return container;
  }

  @Bean
  public TestMessageListener testMessageListener(RabbitTemplateProvider rabbitTemplateProvider) {
    return new TestMessageListener(rabbitTemplateProvider);
  }

  @Slf4j
  @AllArgsConstructor
  public static class TestMessageListener implements ChannelAwareMessageListener {
    public static final String REPLY_MSG_PREFIX = "Reply: ";
    private final RabbitTemplateProvider rabbitTemplateProvider;

    @Override
    public void onMessage(Message message, Channel channel) {
      log.warn("Got message: {} from channel {}", message, channel);
      sendReplyIfRequested(message);
    }

    private void sendReplyIfRequested(Message message) {
      MessageProperties messageProperties = message.getMessageProperties();
      String replyToProperty = messageProperties.getReplyTo();
      if (replyToProperty != null) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getRabbitTemplate();
        Address replyTo = new Address(replyToProperty);
        String replyMsg = REPLY_MSG_PREFIX + new String(message.getBody(), UTF_8);
        Message replyMessage = rabbitTemplate.getMessageConverter().toMessage(replyMsg, null);
        rabbitTemplate.convertAndSend(replyTo.getExchangeName(), replyTo.getRoutingKey(), replyMessage);
      }
    }

  }
}
