package org.janus.sdk.core.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class InMemoryDegradationStateRegistryTest {

  private static final String ID = "deg-1";

  private final InMemoryDegradationStateRegistry registry = new InMemoryDegradationStateRegistry();

  @Test
  void findReturnsEmptyWhenNoStateRegistered() {
    assertThat(registry.find(ID)).isEmpty();
    assertThat(registry.getAll()).isEmpty();
  }

  @Test
  void replaceAllOverwritesPreviousContent() {
    registry.replaceAll(Map.of(ID, state(ID, 0.1)));
    registry.replaceAll(Map.of("other", state("other", 0.2)));

    assertThat(registry.find(ID)).isEmpty();
    assertThat(registry.find("other")).map(DegradationRuntimeState::value).contains(0.2);
  }

  @Test
  void upsertAllMergesWithoutDroppingExistingEntries() {
    registry.replaceAll(Map.of(ID, state(ID, 0.1)));
    registry.upsertAll(Map.of("other", state("other", 0.2)));

    assertThat(registry.getAll()).containsOnlyKeys(ID, "other");
    assertThat(registry.find(ID)).map(DegradationRuntimeState::value).contains(0.1);
    assertThat(registry.find("other")).map(DegradationRuntimeState::value).contains(0.2);
  }

  @Test
  void upsertAllOverwritesExistingValueForSameKey() {
    registry.replaceAll(Map.of(ID, state(ID, 0.1)));
    registry.upsertAll(Map.of(ID, state(ID, 0.9)));

    assertThat(registry.find(ID)).map(DegradationRuntimeState::value).contains(0.9);
  }

  @Test
  void upsertAllOnEmptyMapDoesNothing() {
    registry.replaceAll(Map.of(ID, state(ID, 0.1)));
    registry.upsertAll(Map.of());

    assertThat(registry.find(ID)).isPresent();
  }

  @Test
  void markAllStaleFlagsExistingEntriesWithoutChangingValues() {
    registry.replaceAll(Map.of(ID, state(ID, 0.4), "other", state("other", 0.7)));

    registry.markAllStale();

    assertThat(registry.find(ID))
        .hasValueSatisfying(
            s -> {
              assertThat(s.stale()).isTrue();
              assertThat(s.value()).isEqualTo(0.4);
              assertThat(s.loadedAt()).isEqualTo(Instant.EPOCH);
            });
    assertThat(registry.find("other"))
        .hasValueSatisfying(
            s -> {
              assertThat(s.stale()).isTrue();
              assertThat(s.value()).isEqualTo(0.7);
            });
  }

  @Test
  void markAllStaleOnEmptyRegistryDoesNothing() {
    registry.markAllStale();

    assertThat(registry.getAll()).isEmpty();
  }

  @Test
  void getAllReturnsImmutableSnapshot() {
    registry.replaceAll(Map.of(ID, state(ID, 0.1)));
    Map<String, DegradationRuntimeState> snapshot = registry.getAll();

    org.assertj.core.api.Assertions.assertThatThrownBy(snapshot::clear)
        .isInstanceOf(UnsupportedOperationException.class);
  }

  /**
   * Regression for the race window between {@code clear()} and {@code putAll()} in the previous
   * implementation: a concurrent reader could observe an empty registry mid-replace, which made
   * {@code DegradableAspect} skip the fallback for a degradation that was about to remain in the
   * new snapshot.
   */
  @Test
  void replaceAllIsAtomicForConcurrentReaders() throws InterruptedException {
    var snapshot = Map.of(ID, state(ID, 0.5));
    registry.replaceAll(snapshot);

    var stop = new AtomicBoolean(false);
    var missed = new AtomicLong();
    var ready = new CountDownLatch(1);

    Thread writer =
        new Thread(
            () -> {
              ready.countDown();
              while (!stop.get()) {
                registry.replaceAll(snapshot);
              }
            },
            "registry-writer");

    Thread reader =
        new Thread(
            () -> {
              try {
                ready.await();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
              }
              for (int i = 0; i < 100_000; i++) {
                if (registry.find(ID).isEmpty()) {
                  missed.incrementAndGet();
                }
              }
            },
            "registry-reader");

    writer.start();
    reader.start();

    reader.join(TimeUnit.SECONDS.toMillis(10));
    stop.set(true);
    writer.join(TimeUnit.SECONDS.toMillis(10));

    assertThat(missed.get())
        .as("reader must never observe an absent state when the same key is always present")
        .isZero();
  }

  private static DegradationRuntimeState state(String id, double value) {
    return new DegradationRuntimeState(
        id, value, Duration.ofSeconds(1), 0.5, 0.0, 1.0, 1.0, false, Instant.EPOCH);
  }
}
