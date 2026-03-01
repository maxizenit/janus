package org.janus.sidecar.configuration.properties;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties("janus.sidecar")
@Validated
public record JanusSidecarProperties(@NotNull @DurationMin(millis = 1) Duration policyRefreshInterval) {
}
