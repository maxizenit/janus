package org.janus.evaluator.configuration;

import org.janus.api.policystore.PolicyStoreServiceGrpc;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
@NullMarked
public class PolicyStoreGrpcClientConfiguration {

  @Bean
  public PolicyStoreServiceGrpc.PolicyStoreServiceBlockingStub policyStoreStub(
      GrpcChannelFactory channelFactory) {
    return PolicyStoreServiceGrpc.newBlockingStub(channelFactory.createChannel("policy-store"));
  }
}
