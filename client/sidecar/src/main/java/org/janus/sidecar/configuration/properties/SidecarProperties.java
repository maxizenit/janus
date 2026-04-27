package org.janus.sidecar.configuration.properties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("janus.sidecar")
@Validated
public record SidecarProperties(
    @NotNull @DurationMin(millis = 1) Duration policyRefreshInterval,
    @Positive int stateRefreshThreads,
    @Positive int stateRefreshQueueCapacity,
    @Positive int maxSyncDegradationIds,
    @NotNull String sqlitePath) {}
