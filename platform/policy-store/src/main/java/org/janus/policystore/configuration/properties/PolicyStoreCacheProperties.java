package org.janus.policystore.configuration.properties;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("janus.policy-store.cache")
@Validated
public record PolicyStoreCacheProperties(@NotNull @DurationMin(millis = 1) Duration ttl) {}
