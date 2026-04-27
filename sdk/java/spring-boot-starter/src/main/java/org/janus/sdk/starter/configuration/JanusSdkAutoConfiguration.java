package org.janus.sdk.starter.configuration;

import org.janus.sdk.core.fallback.DefaultFallbackDecisionService;
import org.janus.sdk.core.fallback.FallbackDecisionService;
import org.janus.sdk.core.registry.DegradableMethodRegistry;
import org.janus.sdk.core.registry.InMemoryDegradableMethodRegistry;
import org.janus.sdk.core.runtime.DegradationStateRegistry;
import org.janus.sdk.core.runtime.InMemoryDegradationStateRegistry;
import org.janus.sdk.core.transform.DefaultFallbackArgumentsTransformer;
import org.janus.sdk.core.transform.FallbackArgumentsTransformer;
import org.janus.sdk.core.validation.DefaultDegradableDescriptorValidator;
import org.janus.sdk.core.validation.DegradableDescriptorValidator;
import org.janus.sdk.starter.aop.DegradableAspect;
import org.janus.sdk.starter.aop.DegradableMetrics;
import org.janus.sdk.starter.aop.ReflectionFallbackMethodInvoker;
import org.janus.sdk.starter.client.GrpcSidecarSdkClient;
import org.janus.sdk.starter.configuration.properties.JanusSdkProperties;
import org.janus.sdk.starter.lifecycle.JanusSdkStartupRunner;
import org.janus.sdk.starter.mapper.SidecarRuntimeStateMapper;
import org.janus.sdk.starter.registry.RegistryBackedMethodDescriptorResolver;
import org.janus.sdk.starter.scanner.DegradableDescriptorFactory;
import org.janus.sdk.starter.scanner.DegradableMethodScanner;
import org.janus.sdk.starter.scheduling.DegradationRefreshScheduler;
import java.time.Clock;
import org.janus.sdk.starter.service.DegradationRefreshService;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(JanusSdkProperties.class)
@ConditionalOnProperty(prefix = "janus.sdk", name = "enabled", havingValue = "true")
@Import({
  DegradableAspect.class,
  DegradableMetrics.class,
  ReflectionFallbackMethodInvoker.class,
  RegistryBackedMethodDescriptorResolver.class,
  DegradableMethodScanner.class,
  DegradableDescriptorFactory.class,
  SidecarRuntimeStateMapper.class,
  GrpcSidecarSdkClient.class,
  DegradationRefreshService.class,
  DegradationRefreshScheduler.class,
  JanusSdkStartupRunner.class
})
@NullMarked
public class JanusSdkAutoConfiguration {

  @Bean
  public DegradableDescriptorValidator degradableDescriptorValidator() {
    return new DefaultDegradableDescriptorValidator();
  }

  @Bean
  public DegradableMethodRegistry degradableMethodRegistry() {
    return new InMemoryDegradableMethodRegistry();
  }

  @Bean
  public DegradationStateRegistry degradationStateRegistry() {
    return new InMemoryDegradationStateRegistry();
  }

  @Bean
  public FallbackDecisionService fallbackDecisionService(JanusSdkProperties properties) {
    return new DefaultFallbackDecisionService(properties.staleStrategy());
  }

  @Bean
  public FallbackArgumentsTransformer fallbackArgumentsTransformer() {
    return new DefaultFallbackArgumentsTransformer();
  }

  @Bean
  @ConditionalOnMissingBean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
