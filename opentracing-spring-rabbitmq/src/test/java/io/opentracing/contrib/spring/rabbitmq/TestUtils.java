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

import java.util.Arrays;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.util.ReflectionUtils;

/**
 * Test utility class.
 *
 * @author Gilles Robert
 */
class TestUtils {

  static void verifyNoMoreInteractionsWithMocks(Object testClass) {
    Object[] annotatedMocks = getObjectsAnnotatedWithMock(testClass);
    if (annotatedMocks.length > 0) {
      Mockito.verifyNoMoreInteractions(annotatedMocks);
    }
  }

  private static Object[] getObjectsAnnotatedWithMock(final Object testClass) {
    return Arrays.stream(testClass.getClass().getDeclaredFields())
        .filter(input -> input.isAnnotationPresent(Mock.class))
        .map(
            input -> {
              ReflectionUtils.makeAccessible(input);
              return ReflectionUtils.getField(input, testClass);
            })
        .toArray();
  }
}
