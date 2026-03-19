package org.janus.decider.client.signal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.janus.decider.configuration.properties.PrometheusProperties;
import org.janus.decider.model.snapshot.SignalSourceSnapshot;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
@NullMarked
public class PrometheusSignalClient implements SignalClient {

  private final WebClient prometheusWebClient;
  private final PrometheusProperties properties;
  private final Clock clock;

  @Override
  public double getSignalValue(SignalSourceSnapshot signalSource, Duration evaluationInterval) {
    if (signalSource.type() != SignalSourceSnapshot.SignalSourceType.PROMETHEUS) {
      throw new UnsupportedOperationException(
          "Signal source type is not supported yet: " + signalSource.type());
    }

    var query = resolveQuery(signalSource.reference(), evaluationInterval);

    var body =
        prometheusWebClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/api/v1/query")
                        .queryParam("query", query)
                        .queryParam("time", Instant.now(clock).getEpochSecond())
                        .build())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block(properties.requestTimeout());

    if (body == null) {
      throw new IllegalStateException("Prometheus response body is null");
    }

    return extractSingleValue(body, query);
  }

  private static String resolveQuery(String template, Duration evaluationInterval) {
    return template
        .replace("${evaluation_interval}", toPrometheusDuration(evaluationInterval))
        .replace("${evaluation_interval_s}", Long.toString(evaluationInterval.toSeconds()))
        .replace("${evaluation_interval_ms}", Long.toString(evaluationInterval.toMillis()));
  }

  private static String toPrometheusDuration(Duration duration) {
    var seconds = duration.toSeconds();

    if (seconds % 3600 == 0) {
      return (seconds / 3600) + "h";
    }
    if (seconds % 60 == 0) {
      return (seconds / 60) + "m";
    }
    return seconds + "s";
  }

  private static double extractSingleValue(JsonNode body, String query) {
    var status = body.path("status").asString();
    if (!"success".equals(status)) {
      throw new IllegalStateException("Prometheus query failed: query=" + query + ", body=" + body);
    }

    var data = body.path("data");
    var resultType = data.path("resultType").asString();

    return switch (resultType) {
      case "scalar" -> parseScalarResult(data.path("result"), query);
      case "vector" -> parseVectorResult(data.path("result"), query);
      default ->
          throw new IllegalStateException(
              "Unsupported Prometheus result type: type=" + resultType + ", query=" + query);
    };
  }

  private static double parseScalarResult(JsonNode result, String query) {
    if (!result.isArray() || result.size() < 2) {
      throw new IllegalStateException(
          "Prometheus scalar result must contain timestamp and value: query=" + query);
    }

    return parseDouble(result.get(1).asString(), query);
  }

  private static double parseVectorResult(JsonNode result, String query) {
    if (!result.isArray() || result.size() != 1) {
      throw new IllegalStateException(
          "Prometheus vector result must contain exactly one sample: query="
              + query
              + ", size="
              + result.size());
    }

    var valueNode = result.get(0).path("value");
    if (!valueNode.isArray() || valueNode.size() < 2) {
      throw new IllegalStateException(
          "Prometheus vector value must contain timestamp and value: query=" + query);
    }

    return parseDouble(valueNode.get(1).asString(), query);
  }

  private static double parseDouble(String rawValue, String query) {
    try {
      return Double.parseDouble(rawValue);
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
          "Prometheus returned non-numeric value: query=" + query + ", value=" + rawValue, e);
    }
  }
}
