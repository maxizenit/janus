package org.janus.demo.client.configuration;

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
      RestClient.Builder builder, DemoClientProperties properties) {
    return builder.baseUrl(properties.url().toString()).build();
  }
}
