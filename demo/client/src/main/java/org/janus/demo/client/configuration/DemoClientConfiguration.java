package org.janus.demo.client.configuration;

import io.micrometer.observation.ObservationRegistry;
import org.janus.demo.client.configuration.properties.DemoClientProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@NullMarked
public class DemoClientConfiguration {

  @Bean
  public RestClient demoServerRestClient(
      DemoClientProperties properties, ObservationRegistry observationRegistry) {
    return RestClient.builder()
        .baseUrl(properties.url().toString())
        .observationRegistry(observationRegistry)
        .build();
  }
}
