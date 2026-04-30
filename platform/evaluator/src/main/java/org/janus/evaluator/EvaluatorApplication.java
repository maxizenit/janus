package org.janus.evaluator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class EvaluatorApplication {

  static void main(String[] args) {
    SpringApplication.run(EvaluatorApplication.class, args);
  }
}
