package org.janus.statestore.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.janus.statestore.model.DegradationStateUpdateSource;
import org.junit.jupiter.api.Test;

class DegradationStateUpdateSourceMapperTest {

  private final DegradationStateUpdateSourceMapper mapper = new DegradationStateUpdateSourceMapper();

  @Test
  void fromGrpcToDomain_adminUi() {
    DegradationStateUpdateSource result =
        mapper.fromGrpcToDomain(org.janus.api.statestore.DegradationStateUpdateSource.ADMIN_UI);

    assertThat(result).isEqualTo(DegradationStateUpdateSource.ADMIN_UI);
  }

  @Test
  void fromGrpcToDomain_evaluator() {
    DegradationStateUpdateSource result =
        mapper.fromGrpcToDomain(org.janus.api.statestore.DegradationStateUpdateSource.EVALUATOR);

    assertThat(result).isEqualTo(DegradationStateUpdateSource.EVALUATOR);
  }

  @Test
  void fromGrpcToDomain_unrecognized_returnsNull() {
    DegradationStateUpdateSource result =
        mapper.fromGrpcToDomain(
            org.janus.api.statestore.DegradationStateUpdateSource.UNRECOGNIZED);

    assertThat(result).isNull();
  }

  @Test
  void fromDomainToGrpc_adminUi() {
    org.janus.api.statestore.DegradationStateUpdateSource result =
        mapper.fromDomainToGrpc(DegradationStateUpdateSource.ADMIN_UI);

    assertThat(result)
        .isEqualTo(org.janus.api.statestore.DegradationStateUpdateSource.ADMIN_UI);
  }

  @Test
  void fromDomainToGrpc_evaluator() {
    org.janus.api.statestore.DegradationStateUpdateSource result =
        mapper.fromDomainToGrpc(DegradationStateUpdateSource.EVALUATOR);

    assertThat(result)
        .isEqualTo(org.janus.api.statestore.DegradationStateUpdateSource.EVALUATOR);
  }
}
