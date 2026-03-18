package org.janus.decider.configuration;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.janus.decider.configuration.properties.PrometheusProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@RequiredArgsConstructor
@NullMarked
public class SignalClientConfiguration {

  @Bean
  public WebClient prometheusWebClient(PrometheusProperties properties) {
    var httpClient =
        HttpClient.create()
            .option(
                ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMillis(properties.requestTimeout()))
            .responseTimeout(properties.requestTimeout());

    return WebClient.builder()
        .baseUrl(properties.baseUrl().toString())
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }

  private static int timeoutMillis(Duration duration) {
    return Math.toIntExact(duration.toMillis());
  }
}
