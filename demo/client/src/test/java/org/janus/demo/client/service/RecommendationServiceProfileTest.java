package org.janus.demo.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.janus.demo.client.integration.DemoServerClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

class RecommendationServiceProfileTest {

  @Configuration
  @ComponentScan(basePackages = "org.janus.demo.client.service")
  static class ServicesScanConfig {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(ServicesScanConfig.class)
          .withBean(DemoServerClient.class, () -> mock(DemoServerClient.class));

  @Test
  void defaultProfileLoadsBaseService() {
    runner
        .withPropertyValues("janus.sdk.enabled=false", "resilience4j.enabled=false")
        .run(
            context -> {
              assertThat(context).hasSingleBean(RecommendationService.class);
              assertThat(context.getBean(RecommendationService.class))
                  .isInstanceOf(BaseRecommendationService.class);
              assertThat(context).doesNotHaveBean(JanusRecommendationService.class);
              assertThat(context).doesNotHaveBean(Resilience4jRecommendationService.class);
            });
  }

  @Test
  void janusProfileLoadsJanusService() {
    runner
        .withInitializer(ctx -> ctx.getEnvironment().setActiveProfiles("janus"))
        .withPropertyValues("janus.sdk.enabled=true", "resilience4j.enabled=false")
        .run(
            context -> {
              assertThat(context).hasSingleBean(RecommendationService.class);
              assertThat(context.getBean(RecommendationService.class))
                  .isInstanceOf(JanusRecommendationService.class);
              assertThat(context).doesNotHaveBean(BaseRecommendationService.class);
              assertThat(context).doesNotHaveBean(Resilience4jRecommendationService.class);
            });
  }

  @Test
  void r4jProfileLoadsResilience4jService() {
    runner
        .withInitializer(ctx -> ctx.getEnvironment().setActiveProfiles("r4j"))
        .withPropertyValues("janus.sdk.enabled=false", "resilience4j.enabled=true")
        .run(
            context -> {
              assertThat(context).hasSingleBean(RecommendationService.class);
              assertThat(context.getBean(RecommendationService.class))
                  .isInstanceOf(Resilience4jRecommendationService.class);
              assertThat(context).doesNotHaveBean(BaseRecommendationService.class);
              assertThat(context).doesNotHaveBean(JanusRecommendationService.class);
            });
  }
}
