package org.janus.decider.configuration.properties;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("janus.decider")
@Validated
public record JanusDeciderProperties(@NotNull @DurationMin(millis = 1) Duration policyRefreshInterval) {}
