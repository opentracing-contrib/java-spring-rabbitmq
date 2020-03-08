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
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ats Uiboupin
 * @author Gilles Robert
 */
@Configuration
@ImportAutoConfiguration(RabbitTemplateProviderConfig.class)
public class RabbitWithoutRabbitTemplateConfig {

  @Bean
  public Queue queue() {
    return new Queue("myQueue", false);
  }

  @Bean
  public RabbitConnectionFactoryBean rabbitConnectionFactoryBean() {
    RabbitConnectionFactoryBean rabbitConnectionFactoryBean = new RabbitConnectionFactoryBean();
    rabbitConnectionFactoryBean.setUsername("guest");
    rabbitConnectionFactoryBean.setPassword("guest");
    rabbitConnectionFactoryBean.setHost(System.getProperty("spring.rabbitmq.host"));
    rabbitConnectionFactoryBean.setVirtualHost("/");
    rabbitConnectionFactoryBean.setPort(Integer.valueOf(System.getProperty("spring.rabbitmq.port")));
    rabbitConnectionFactoryBean.afterPropertiesSet();
    return rabbitConnectionFactoryBean;
  }

  @Bean
  public CachingConnectionFactory cachingConnectionFactory(
      RabbitConnectionFactoryBean rabbitConnectionFactoryBean) throws Exception {
    return new CachingConnectionFactory(rabbitConnectionFactoryBean.getObject());
  }

  @Bean
  public RabbitAdmin rabbitAdmin(Queue queue, ConnectionFactory connectionFactory) {
    final TopicExchange exchange = new TopicExchange("myExchange", true, false);

    final RabbitAdmin admin = new RabbitAdmin(connectionFactory);
    admin.declareQueue(queue);
    admin.declareExchange(exchange);
    admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("#"));

    return admin;
  }

  @Bean
  public SimpleMessageListenerContainer messageListenerContainer(
      TestMessageListener messageListener, ConnectionFactory connectionFactory, Queue queue) {
    SimpleMessageListenerContainer container =
        new SimpleMessageListenerContainer(connectionFactory);
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

    public static final String HEADER_CUSTOM_RESPONSE_ERROR_MARKER_HEADER = "custom.error";
    static final String REPLY_MSG_PREFIX = "Reply: ";
    static final String HEADER_SLEEP_MILLIS = "sleep.millis";
    static final String HEADER_ADD_CUSTOM_ERROR_HEADER_TO_RESPONSE = "add.custom.error";
    private final RabbitTemplateProvider rabbitTemplateProvider;

    @Override
    public void onMessage(Message message, Channel channel) {
      log.info("Got message: {} from channel {}", message, channel);
      sleepIfRequested((Long) message.getMessageProperties().getHeaders().get(HEADER_SLEEP_MILLIS));
      sendReplyIfRequested(message);
    }

    private void sleepIfRequested(Long sleepMillis) {
      if (sleepMillis != null) {
        try {
          log.info("Sleeping {}ms as requested", sleepMillis);
          Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
      }
    }

    private void sendReplyIfRequested(Message message) {
      MessageProperties messageProperties = message.getMessageProperties();
      String replyToProperty = messageProperties.getReplyTo();

      if (replyToProperty != null) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getRabbitTemplate();
        Address replyTo = new Address(replyToProperty);
        String replyMsg = REPLY_MSG_PREFIX + new String(message.getBody(), UTF_8);
        Message replyMessage = rabbitTemplate.getMessageConverter().toMessage(replyMsg, null);

        Object addCustomResponseErrorMarkerHeader = messageProperties.getHeaders()
            .get(HEADER_ADD_CUSTOM_ERROR_HEADER_TO_RESPONSE);

        if (addCustomResponseErrorMarkerHeader != null) {
          replyMessage.getMessageProperties().setHeader(HEADER_CUSTOM_RESPONSE_ERROR_MARKER_HEADER, "dummy error message");
        }
        replyMessage.getMessageProperties().setCorrelationId(messageProperties.getCorrelationId());

        rabbitTemplate.convertAndSend(replyTo.getExchangeName(), replyTo.getRoutingKey(), replyMessage);
      }
    }

  }
}
