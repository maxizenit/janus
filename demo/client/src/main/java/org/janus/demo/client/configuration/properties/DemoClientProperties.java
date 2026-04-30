package org.janus.demo.client.configuration.properties;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("demo.server")
@Validated
public record DemoClientProperties(
    @NotNull URI url,
    @NotNull @DurationMin(millis = 1) Duration connectTimeout,
    @NotNull @DurationMin(millis = 1) Duration readTimeout) {}
