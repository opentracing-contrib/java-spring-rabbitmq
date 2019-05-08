# Changelog

## [2.0.1](https://github.com/opentracing-contrib/java-spring-rabbitmq/milestone/6) - 2019-05-08
- [Fix tracing auto configuration](https://github.com/opentracing-contrib/java-spring-rabbitmq/pull/34): [@ask4gilles](https://github.com/ask4gilles)

## [2.0.0](https://github.com/opentracing-contrib/java-spring-rabbitmq/milestone/5) - 2019-05-02
- [Dependencies upgrade](https://github.com/opentracing-contrib/java-spring-rabbitmq/pull/31): [@ask4gilles](https://github.com/ask4gilles)
- [Bump SB version and use testcontainers for rabbitMQ IT tests](https://github.com/opentracing-contrib/java-spring-rabbitmq/pull/30): [@ask4gilles](https://github.com/ask4gilles)

## [1.0.1](https://github.com/opentracing-contrib/java-spring-rabbitmq/milestone/4) - 2019-03-20
- [Add instrumentation method](https://github.com/opentracing-contrib/java-spring-rabbitmq/pull/26): [@ask4gilles](https://github.com/ask4gilles)
- [Update checkstyle](https://github.com/opentracing-contrib/java-spring-rabbitmq/issues/24): [@ask4gilles](https://github.com/ask4gilles)

## [1.0.0](https://github.com/opentracing-contrib/java-spring-rabbitmq/milestone/2) - 2019-03-13

### Code breaking
- [Upgrade to spring boot 2.1+](https://github.com/opentracing-contrib/java-spring-rabbitmq/issues/17): [@sudr](https://github.com/sudr)

### Improvements
- [Support for DirectMessageListenerContainer](https://github.com/opentracing-contrib/java-spring-rabbitmq/issues/13): [@sudr](https://github.com/sudr)

## [0.1.1](https://github.com/opentracing-contrib/java-spring-rabbitmq/milestone/1) - 2019-03-12

### Improvements
- [#8](https://github.com/opentracing-contrib/java-spring-rabbitmq/issues/8): Improvement: allow reusing code from this library to inject & extract tracing info
- [#11](https://github.com/opentracing-contrib/java-spring-rabbitmq/issues/11): Support for tracing other AmqpTemplate send methods [@sudr](https://github.com/sudr) 
to AMQP messages when application doesn't have RabbitTemplate bean [@atsu85](https://github.com/atsu85)
- [#15](https://github.com/opentracing-contrib/java-spring-rabbitmq/issues/15): Enhance RabbitMqSendTracingAspect to support convertAndSend with routingKey,message,messagePostProcessor,correlationData arguments [@sudr](https://github.com/sudr)
- [#18](https://github.com/opentracing-contrib/java-spring-rabbitmq/issues/18): Rename span.kind to produce/consumer [@ask4gilles](https://github.com/ask4gilles)

