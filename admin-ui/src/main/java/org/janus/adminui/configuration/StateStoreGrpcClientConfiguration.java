package org.janus.adminui.configuration;

import io.grpc.ClientInterceptors;
import java.time.Duration;
import org.janus.api.statestore.StateStoreServiceGrpc;
import org.janus.common.grpc.DefaultDeadlineInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class StateStoreGrpcClientConfiguration {

  @Bean
  public StateStoreServiceGrpc.StateStoreServiceBlockingStub stateStoreStub(
      GrpcChannelFactory channelFactory,
      @Value("${janus.admin-ui.grpc.client.default-deadline:5s}") Duration defaultDeadline) {
    var channel =
        ClientInterceptors.intercept(
            channelFactory.createChannel("state-store"),
            new DefaultDeadlineInterceptor(defaultDeadline));
    return StateStoreServiceGrpc.newBlockingStub(channel);
  }
}
