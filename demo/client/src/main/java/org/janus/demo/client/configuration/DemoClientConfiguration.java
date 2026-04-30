package org.janus.demo.client.configuration;

import io.micrometer.observation.ObservationRegistry;
import java.net.http.HttpClient;
import org.janus.demo.client.configuration.properties.DemoClientProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@NullMarked
public class DemoClientConfiguration {

  @Bean
  public RestClient demoServerRestClient(
      DemoClientProperties properties, ObservationRegistry observationRegistry) {
    var httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.readTimeout());

    return RestClient.builder()
        .baseUrl(properties.url().toString())
        .requestFactory(requestFactory)
        .observationRegistry(observationRegistry)
        .build();
  }
}
