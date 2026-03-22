package org.janus.adminui.configuration;

import org.janus.api.statestore.StateStoreServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class StateStoreGrpcClientConfiguration {

  @Bean
  public StateStoreServiceGrpc.StateStoreServiceBlockingStub stateStoreStub(
      GrpcChannelFactory channelFactory) {
    return StateStoreServiceGrpc.newBlockingStub(channelFactory.createChannel("state-store"));
  }
}
