/*
 * Copyright 2014-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.utils.serializer.serializers;

import static org.assertj.core.api.Assertions.assertThat;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AtomicSerializersTest {
  private static final int CAPACITY = 1024;
  private static final Kryo KRYO = new Kryo();

  private Output output;
  private Input input;
  private ByteBuffer buffer;

  @BeforeClass
  public static void register() {
    KRYO.register(AtomicLong.class, new AtomicLongSerializer());
    KRYO.register(AtomicInteger.class, new AtomicIntegerSerializer());
    KRYO.register(AtomicBoolean.class, new AtomicBooleanSerializer());
  }

  @Before
  public void setUp() {
    buffer = ByteBuffer.allocate(CAPACITY);

    output = new ByteBufferOutput(buffer);
    input = new ByteBufferInput(buffer);
  }

  @After
  public void tearDown() {
    output.close();
    input.close();
  }

  @Test
  public void shouldSerializeDeserializeLong() {
    // given
    final AtomicLong original = new AtomicLong(1);

    // when
    original.set(32L);
    KRYO.writeObject(output, original);
    buffer.flip();

    final AtomicLong deserialized = KRYO.readObject(input, AtomicLong.class);

    // then
    assertThat(deserialized.get()).isEqualTo(32L);
  }

  @Test
  public void shouldSerializeDeserializeInteger() {
    // given
    final AtomicInteger original = new AtomicInteger(1);

    // when
    original.set(1000);
    KRYO.writeObject(output, original);
    buffer.flip();
    final AtomicInteger deserialized = KRYO.readObject(input, AtomicInteger.class);

    // then
    assertThat(deserialized.get()).isEqualTo(1000);
  }

  @Test
  public void shouldSerializeDeserializeBoolean() {
    // given
    final AtomicBoolean original = new AtomicBoolean(false);

    // when
    original.set(true);
    KRYO.writeObject(output, original);
    buffer.flip();
    final AtomicBoolean deserialized = KRYO.readObject(input, AtomicBoolean.class);

    // then
    assertThat(deserialized.get()).isTrue();
  }
}
