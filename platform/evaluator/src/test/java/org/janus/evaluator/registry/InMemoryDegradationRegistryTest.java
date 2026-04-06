package org.janus.evaluator.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryDegradationRegistryTest {

  private InMemoryDegradationRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new InMemoryDegradationRegistry();
  }

  @Test
  void sync_addsNewDegradations() {
    registry.sync(Set.of("d1", "d2"));

    assertThat(registry.find("d1")).isPresent();
    assertThat(registry.find("d2")).isPresent();
    assertThat(registry.size()).isEqualTo(2);
  }

  @Test
  void sync_removesAbsentDegradations() {
    registry.sync(Set.of("d1", "d2", "d3"));
    assertThat(registry.size()).isEqualTo(3);

    registry.sync(Set.of("d1"));

    assertThat(registry.find("d1")).isPresent();
    assertThat(registry.find("d2")).isEmpty();
    assertThat(registry.find("d3")).isEmpty();
    assertThat(registry.size()).isEqualTo(1);
  }

  @Test
  void sync_removedDegradationsAreDeactivated() {
    registry.sync(Set.of("d1"));
    var holder = registry.find("d1");
    assertThat(holder).isPresent();
    assertThat(holder.get().isActive()).isTrue();

    registry.sync(Set.of());

    assertThat(holder.get().isActive()).isFalse();
  }

  @Test
  void sync_addsNewAndRemovesOldSimultaneously() {
    registry.sync(Set.of("d1", "d2"));
    registry.sync(Set.of("d2", "d3"));

    assertThat(registry.find("d1")).isEmpty();
    assertThat(registry.find("d2")).isPresent();
    assertThat(registry.find("d3")).isPresent();
    assertThat(registry.size()).isEqualTo(2);
  }

  @Test
  void find_returnsEmptyForUnknownId() {
    assertThat(registry.find("nonexistent")).isEmpty();
  }

  @Test
  void find_returnsEmptyForInactiveDegradation() {
    registry.sync(Set.of("d1"));
    var holder = registry.find("d1").orElseThrow();
    holder.deactivate();

    assertThat(registry.find("d1")).isEmpty();
  }

  @Test
  void findAllActive_returnsOnlyActive() {
    registry.sync(Set.of("d1", "d2", "d3"));
    registry.find("d2").orElseThrow().deactivate();

    var active = registry.findAllActive();
    assertThat(active).hasSize(2);
    assertThat(active).extracting("degradationId").containsExactlyInAnyOrder("d1", "d3");
  }

  @Test
  void findAllActive_emptyRegistry_returnsEmptyList() {
    assertThat(registry.findAllActive()).isEmpty();
  }

  @Test
  void size_countsOnlyActive() {
    registry.sync(Set.of("d1", "d2"));
    assertThat(registry.size()).isEqualTo(2);

    registry.find("d1").orElseThrow().deactivate();
    assertThat(registry.size()).isEqualTo(1);
  }

  @Test
  void sync_emptySet_removesAll() {
    registry.sync(Set.of("d1", "d2"));
    registry.sync(Set.of());

    assertThat(registry.size()).isEqualTo(0);
    assertThat(registry.findAllActive()).isEmpty();
  }

  @Test
  void sync_sameSet_noChange() {
    registry.sync(Set.of("d1", "d2"));
    var holderBefore = registry.find("d1").orElseThrow();

    registry.sync(Set.of("d1", "d2"));
    var holderAfter = registry.find("d1").orElseThrow();

    assertThat(holderAfter).isSameAs(holderBefore);
    assertThat(registry.size()).isEqualTo(2);
  }
}
