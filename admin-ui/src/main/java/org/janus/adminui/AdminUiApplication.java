package org.janus.adminui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AdminUiApplication {

  static void main(String[] args) {
    SpringApplication.run(AdminUiApplication.class, args);
  }
}
