package org.janus.sdk.starter.configuration;

import io.grpc.ClientInterceptors;
import java.time.Duration;
import org.janus.api.sidecar.SidecarServiceGrpc;
import org.janus.common.grpc.DefaultDeadlineInterceptor;
import org.janus.sdk.starter.configuration.properties.JanusSdkProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
@ConditionalOnJanusSdkEnabled
@NullMarked
public class SidecarGrpcClientConfiguration {

  @Bean
  public SidecarServiceGrpc.SidecarServiceBlockingStub sidecarStub(
      GrpcChannelFactory channelFactory,
      JanusSdkProperties properties,
      @Value("${janus.sdk.grpc.client.default-deadline:5s}") Duration defaultDeadline) {
    var channel =
        ClientInterceptors.intercept(
            channelFactory.createChannel(properties.sidecarChannel()),
            new DefaultDeadlineInterceptor(defaultDeadline));
    return SidecarServiceGrpc.newBlockingStub(channel);
  }
}
