package org.janus.demo.client.configuration.properties;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("demo.server")
@Validated
public record DemoClientProperties(@NotNull URI url) {}
