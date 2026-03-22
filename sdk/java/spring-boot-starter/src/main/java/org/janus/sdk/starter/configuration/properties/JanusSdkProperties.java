package org.janus.sdk.starter.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("janus.sdk")
@Validated
public record JanusSdkProperties(
    boolean enabled,
    @NotBlank String sidecarChannel,
    @NotNull @DurationMin(millis = 1) Duration refreshInterval,
    @NotNull @DurationMin(millis = 1) Duration initialRefreshTimeout) {}
