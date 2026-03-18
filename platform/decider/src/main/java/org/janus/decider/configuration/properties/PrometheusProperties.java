package org.janus.decider.configuration.properties;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("janus.decider.prometheus")
@Validated
public record PrometheusProperties(
    @NotNull URI baseUrl, @NotNull @DurationMin(millis = 1) Duration requestTimeout) {}
