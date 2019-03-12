# Changelog

## [0.1.1](https://github.com/opentracing-contrib/java-spring-rabbitmq/milestone/1) - 2019-03-12

### Improvements
- [#8](https://github.com/opentracing-contrib/java-spring-rabbitmq/issues/8): Improvement: allow reusing code from this library to inject & extract tracing info
- [#11](https://github.com/opentracing-contrib/java-spring-rabbitmq/issues/11): Support for tracing other AmqpTemplate send methods [@sudr](https://github.com/sudr) 
to AMQP messages when application doesn't have RabbitTemplate bean [@atsu85](https://github.com/atsu85)
- [#15](https://github.com/opentracing-contrib/java-spring-rabbitmq/issues/15): Enhance RabbitMqSendTracingAspect to support convertAndSend with routingKey,message,messagePostProcessor,correlationData arguments [@sudr](https://github.com/sudr)
- [#18](https://github.com/opentracing-contrib/java-spring-rabbitmq/issues/18): Rename span.kind to produce/consumer [@ask4gilles](https://github.com/ask4gilles)

