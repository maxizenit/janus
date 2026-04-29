package org.janus.sdk.starter.configuration;

import org.janus.api.sidecar.SidecarServiceGrpc;
import org.janus.sdk.starter.configuration.properties.JanusSdkProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
@ConditionalOnJanusSdkEnabled
@NullMarked
public class SidecarGrpcClientConfiguration {

  @Bean
  public SidecarServiceGrpc.SidecarServiceBlockingStub sidecarStub(
      GrpcChannelFactory channelFactory, JanusSdkProperties properties) {
    return SidecarServiceGrpc.newBlockingStub(
        channelFactory.createChannel(properties.sidecarChannel()));
  }
}
