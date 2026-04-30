package org.janus.sidecar.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.janus.sidecar.model.RegisteredDegradation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActualDegradationRegistryTest {

  private InMemoryActualDegradationRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new InMemoryActualDegradationRegistry();
  }

  @Test
  void sync_registersNewDegradations() {
    var result = registry.sync(Set.of("deg-1", "deg-2"));

    assertThat(result.addedIds()).containsExactlyInAnyOrder("deg-1", "deg-2");
    assertThat(result.removedIds()).isEmpty();
    assertThat(result.retainedIds()).isEmpty();
  }

  @Test
  void findAllActive_returnsRegisteredDegradations() {
    registry.sync(Set.of("deg-1", "deg-2"));

    var active = registry.findAllActive();

    assertThat(active)
        .hasSize(2)
        .extracting(RegisteredDegradation::getDegradationId)
        .containsExactlyInAnyOrder("deg-1", "deg-2");
  }

  @Test
  void sync_addsNewAndDeactivatesRemoved() {
    registry.sync(Set.of("deg-1", "deg-2"));

    var result = registry.sync(Set.of("deg-2", "deg-3"));

    assertThat(result.addedIds()).containsExactly("deg-3");
    assertThat(result.removedIds()).containsExactly("deg-1");
    assertThat(result.retainedIds()).containsExactly("deg-2");

    var active = registry.findAllActive();
    assertThat(active)
        .extracting(RegisteredDegradation::getDegradationId)
        .containsExactlyInAnyOrder("deg-2", "deg-3");
  }

  @Test
  void sync_removesAll_whenDesiredIdsEmpty() {
    registry.sync(Set.of("deg-1", "deg-2"));

    var result = registry.sync(Set.of());

    assertThat(result.addedIds()).isEmpty();
    assertThat(result.removedIds()).containsExactlyInAnyOrder("deg-1", "deg-2");
    assertThat(registry.findAllActive()).isEmpty();
  }

  @Test
  void find_returnsActiveDegradation() {
    registry.sync(Set.of("deg-1"));

    var found = registry.find("deg-1");

    assertThat(found).isPresent();
    assertThat(found.get().getDegradationId()).isEqualTo("deg-1");
  }

  @Test
  void find_returnsEmptyForUnknownId() {
    assertThat(registry.find("unknown")).isEmpty();
  }

  @Test
  void size_countsActiveDegradations() {
    registry.sync(Set.of("deg-1", "deg-2", "deg-3"));
    assertThat(registry.size()).isEqualTo(3);

    registry.sync(Set.of("deg-1"));
    assertThat(registry.size()).isEqualTo(1);
  }
}
