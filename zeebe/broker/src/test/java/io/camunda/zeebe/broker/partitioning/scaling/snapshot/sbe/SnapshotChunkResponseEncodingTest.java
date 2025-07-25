/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.broker.partitioning.scaling.snapshot.sbe;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.broker.partitioning.scaling.snapshot.SnapshotChunkRecord;
import io.camunda.zeebe.broker.partitioning.scaling.snapshot.SnapshotResponse.SnapshotChunkResponse;
import io.camunda.zeebe.snapshots.SnapshotChunk;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class SnapshotChunkResponseEncodingTest {
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void shouldEncodeAndDecodeSnapshotChunkResponse(final boolean isEmpty) {
    // given
    final var content = "this is the chunk content".getBytes();
    final Optional<SnapshotChunk> chunk =
        isEmpty
            ? Optional.empty()
            : Optional.of(
                new SnapshotChunkRecord("123123", 23, "a chunk", 23, content, 39, 129213));
    final var snapshotChunkResponse = new SnapshotChunkResponse(UUID.randomUUID(), chunk);
    final var serializer = new SnapshotChunkResponseSerializer();
    final var deserializer = new SnapshotChunkResponseDeserializer();
    final var buffer = ByteBuffer.allocate(4096);

    // when
    final var bytesWritten =
        serializer.serialize(snapshotChunkResponse, new UnsafeBuffer(buffer), 39);
    final var deserialized = deserializer.deserialize(new UnsafeBuffer(buffer), 39, bytesWritten);

    assertThat(deserialized).isEqualTo(snapshotChunkResponse);
  }
}
