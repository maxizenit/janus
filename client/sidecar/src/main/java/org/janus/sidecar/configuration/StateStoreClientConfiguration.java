package org.janus.sidecar.configuration;

import org.janus.api.statestore.StateStoreServiceGrpc;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
@NullMarked
public class StateStoreClientConfiguration {

  @Bean
  public StateStoreServiceGrpc.StateStoreServiceBlockingStub stateStoreStub(
      GrpcChannelFactory channelFactory) {
    return StateStoreServiceGrpc.newBlockingStub(channelFactory.createChannel("state-store"));
  }
}
