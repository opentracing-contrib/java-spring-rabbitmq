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

import io.opentracing.tag.StringTag;
import io.opentracing.tag.Tags;

/**
 * @author Gilles Robert
 */
public final class RabbitMqTracingTags {

  public static final StringTag RABBITMQ = new StringTag("rabbitmq");
  public static final StringTag MESSAGE_ID = new StringTag("messageid");
  public static final StringTag ROUTING_KEY = new StringTag("routingkey");
  public static final StringTag CONSUMER_QUEUE = new StringTag("consumerqueue");
  public static final StringTag EXCHANGE = new StringTag("exchange");
  public static final String SPAN_KIND_PRODUCER = Tags.SPAN_KIND_PRODUCER;
  public static final String SPAN_KIND_CONSUMER = Tags.SPAN_KIND_CONSUMER;

  private RabbitMqTracingTags() {}
}
