package org.janus.demo.client.configuration;

import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@NullMarked
public class DemoClientConfiguration {

  @Bean
  public RestClient demoServerRestClient() {
    return RestClient.builder().baseUrl("http://localhost:8090").build();
  }
}
