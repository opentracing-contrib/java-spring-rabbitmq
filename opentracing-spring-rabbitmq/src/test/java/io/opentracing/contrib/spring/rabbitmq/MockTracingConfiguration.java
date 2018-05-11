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

import io.opentracing.mock.MockTracer;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;

import java.lang.reflect.Field;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *  @author Pavol Loffay
 */
@Configuration
@EnableAutoConfiguration
public class MockTracingConfiguration {

  private static void resetGlobalTracer() {
    try {
      Field globalTracerField = GlobalTracer.class.getDeclaredField("tracer");
      globalTracerField.setAccessible(true);
      globalTracerField.set(null, NoopTracerFactory.create());
      globalTracerField.setAccessible(false);
    } catch (Exception e) {
      throw new RuntimeException("Error reflecting globalTracer: " + e.getMessage(), e);
    }
  }

  @Bean
  public MockTracer mockTracer() {
    resetGlobalTracer();
    return new MockTracer();
  }
}
