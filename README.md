[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

# OpenTracing Spring RabbitMQ

This repository provides OpenTracing instrumentation for RabbitMQ. It can be used with any OpenTracing compatible implementation.

The 0.x.x version of this library is compatible with Spring Boot 1.5.x (see [release-0.1](https://github.com/opentracing-contrib/java-spring-rabbitmq/tree/release-0.1) branch)

The 1.x.x and 2.x.x versions of this library is compatible with Spring Boot 2.1.x


## Configuration

> **Note**: make sure that an `io.opentracing.Tracer` bean is available. It is not provided by this library.

### Spring Boot
Add the following starter dependency to your pom.xml:
```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-rabbitmq-starter</artifactId>
</dependency>
```

### Spring
Add the following dependency to your pom.xml:
```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-rabbitmq</artifactId>
</dependency>
```

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
