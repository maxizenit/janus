package org.janus.adminui.configuration;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminUiConfiguration {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
