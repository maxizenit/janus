package org.janus.adminui.configuration.properties;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("janus.admin-ui")
@Validated
public record AdminUiProperties(
    @NotNull @DurationMin(seconds = 1) Duration stateRefreshInterval,
    @NotNull @DurationMin(seconds = 1) @DurationMax(hours = 24) Duration maxOverrideTtl) {}
