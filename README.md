[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

# OpenTracing Spring RabbitMQ

This repository provides OpenTracing instrumentation for RabbitMQ. It can be used with any OpenTracing compatible implementation.

## Compatibility table

Version | OpenTracing API | Spring Boot version
--- | --- | ---
0.x.x | 0.31.0 | 1.5.x
1.x.x | 0.31.0 | 2.1.x
1.0.2 | 0.32.0 | 2.1.x
2.0.x | 0.32.0 | 2.1.x

## Configuration

> **Note**: make sure that an `io.opentracing.Tracer` bean is available. It is not provided by this library.

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
