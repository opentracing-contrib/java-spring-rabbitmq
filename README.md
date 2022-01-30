![maintenance-status](https://img.shields.io/badge/maintenance-deprecated-red.svg)
[![No Maintenance Intended](http://unmaintained.tech/badge.svg)](http://unmaintained.tech/)
[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

# OpenTracing Spring RabbitMQ

Provides message tracing for RabbitMQ through [Spring AMQP](https://github.com/spring-projects/spring-amqp).
It can be used with any OpenTracing compatible implementation.

## Compatibility table

Version | OpenTracing API | Spring Boot version
--- | --- | ---
0.x.x | 0.31.0 | 1.5.x
1.x.x | 0.31.0 | 2.1.x
1.0.2 | 0.32.0 | 2.1.x
2.0.x | 0.32.0 | 2.1.x
3.0.x | 0.33.0 | 2.1.x

## Sent messages
The following methods are instrumented:

Class | Method | Instrumented
--- | --- | --- 
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `void send(Message message)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `void send(String routingKey, Message message)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `void send(String exchange, String routingKey, Message message)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `void convertAndSend(Object message)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `void convertAndSend(String routingKey, Object message)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `void convertAndSend(String exchange, String routingKey, Object message)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `void convertAndSend(Object message, MessagePostProcessor messagePostProcessor)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `void convertAndSend(String routingKey, Object message, MessagePostProcessor messagePostProcessor)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `void convertAndSend(String exchange, String routingKey, Object message, MessagePostProcessor messagePostProcessor)` |  &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `void convertAndSend(String exchange, String routingKey, Object message, @Nullable CorrelationData correlationData)` |  &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `Message sendAndReceive(Message message)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `Message sendAndReceive(String routingKey, Message message)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `Message sendAndReceive(String exchange, String routingKey, Message message)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `Object convertSendAndReceive(Object message)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `Object convertSendAndReceive(String routingKey, Object message)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `Object convertSendAndReceive(String exchange, String routingKey, Object message)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `Object convertSendAndReceive(Object message, MessagePostProcessor messagePostProcessor)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `Object convertSendAndReceive(String routingKey, Object message, MessagePostProcessor messagePostProcessor)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `Object convertSendAndReceive(String exchange, String routingKey, Object message, MessagePostProcessor messagePostProcessor)` | &#10004;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `<T> T convertSendAndReceiveAsType(Object message, ParameterizedTypeReference<T> responseType)` | &#10060;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `<T> T convertSendAndReceiveAsType(String routingKey, Object message,ParameterizedTypeReference<T> responseType)` | &#10060;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `<T> T convertSendAndReceiveAsType(String routingKey, Object message,  MessagePostProcessor messagePostProcessor, ParameterizedTypeReference<T> responseType)` | &#10060;
[AmqpTemplate](https://docs.spring.io/spring-amqp/api/index.html?org/springframework/amqp/core/AmqpTemplate.html) | `<T> T convertSendAndReceiveAsType(String exchange, String routingKey, Object message,MessagePostProcessor messagePostProcessor, ParameterizedTypeReference<T> responseType)` | &#10060;
[RabbitTemplate](https://docs.spring.io/spring-amqp/api/org/springframework/amqp/rabbit/core/RabbitTemplate.html) | `Message sendAndReceive(final String exchange, final String routingKey, final Message message, @Nullable CorrelationData correlationData)` | &#10004;
[RabbitTemplate](https://docs.spring.io/spring-amqp/api/org/springframework/amqp/rabbit/core/RabbitTemplate.html) | `Object convertSendAndReceive(final String exchange, final String routingKey, final Object message, @Nullable CorrelationData correlationData)` | &#10004;

## Received messages
At startup a `RabbitMqReceiveTracingInterceptor`, will be added to`SimpleMessageListenerContainer` or
`DirectMessageListenerContainer`advice chain, depending on your configuration.

 `@RabbitListener` will also benefit from it.
 
## Span decorator
By default, a `RabbitMqSpanDecorator` is provided, with the following attributes:
### On send span
* component: rabbitmq
* exchange: [exchange_name]
* messageid: [message_id]
* routingkey: [routing_key]

### On receive span
* component: rabbitmq
* exchange: [exchange_name]
* messageid: [message_id]
* routingkey: [routing_key]
* consumerqueue: [consumer_queue]

### On send reply
Nothing by default.

### On error
* event: error key
* error.object: exception

> **Note**: you can customize your spans by declaring an overridden `RabbitMqSpanDecorator` bean.

## Usage

This library is embedded in [java-spring-cloud](https://github.com/opentracing-contrib/java-spring-cloud)

### Usage with Jaeger tracer
If you want to use [Jaeger](https://www.jaegertracing.io/) as tracer,
you can benefit directly from it by importing [java-spring-jaeger](https://github.com/opentracing-contrib/java-spring-jaeger).

```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-jaeger-cloud-starter</artifactId>
</dependency>
```

### Usage with Zipkin tracer
If you want to use [Zipkin](https://zipkin.io/) as tracer, 
you can benefit directly from it by importing [java-spring-zipkin](https://github.com/opentracing-contrib/java-spring-zipkin).

```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-zipkin-cloud-starter</artifactId>
</dependency>
```

### Standalone usage
> **Note**: make sure that an `io.opentracing.Tracer` bean is available. **It is not provided by this library**.
#### With Spring Boot
Add the following starter dependency to your pom.xml:
```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-rabbitmq-starter</artifactId>
</dependency>
```

#### With Spring
Add the following dependency to your pom.xml:
```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-rabbitmq</artifactId>
</dependency>
```
## Jaeger UI example
![Alt text](img/produce-consumer-jaeger.png?raw=true "Jaeger UI")
## Development
Maven checkstyle plugin is used to maintain consistent code style based on [Google Style Guides](https://github.com/google/styleguide)
```shell
./mvnw clean install
```

## Release
Follow instructions in [RELEASE](RELEASE.md)

[ci-img]: https://travis-ci.org/opentracing-contrib/java-spring-rabbitmq.svg?branch=master
[ci]: https://travis-ci.org/opentracing-contrib/java-spring-rabbitmq
[maven-img]: https://img.shields.io/maven-central/v/io.opentracing.contrib/opentracing-spring-rabbitmq.svg?maxAge=2592000
[maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-spring-rabbitmq
