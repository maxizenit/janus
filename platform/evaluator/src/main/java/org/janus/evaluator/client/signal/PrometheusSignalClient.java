package org.janus.evaluator.client.signal;

import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.evaluator.configuration.properties.PrometheusProperties;
import org.janus.evaluator.model.snapshot.SignalSourceSnapshot;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class PrometheusSignalClient implements SignalClient {

  private final WebClient prometheusWebClient;
  private final PrometheusProperties properties;
  private final Clock clock;

  @Override
  public double getSignalValue(SignalSourceSnapshot signalSource) {
    if (signalSource.type() != SignalSourceSnapshot.SignalSourceType.PROMETHEUS) {
      log.error(
          "Unsupported signal source type for Prometheus client: signalSourceType={}, query={}",
          signalSource.type(),
          signalSource.query());
      throw new UnsupportedOperationException(
          "Signal source type is not supported yet: " + signalSource.type());
    }

    var query = signalSource.query();
    var queryTime = Instant.now(clock);

    log.debug(
        "Prometheus query started: query={}, timeout={}",
        query,
        properties.requestTimeout());

    var body =
        prometheusWebClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/api/v1/query")
                        .queryParam("query", query)
                        .queryParam("time", queryTime.getEpochSecond())
                        .build())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block(properties.requestTimeout());

    if (body == null) {
      log.error("Prometheus response body is null: query={}", query);
      throw new IllegalStateException("Prometheus response body is null");
    }

    var resultType = body.path("data").path("resultType").asString();
    log.debug("Prometheus query completed: query={}, resultType={}", query, resultType);

    var value = extractSingleValue(body, query);
    log.debug("Prometheus value extracted: query={}, value={}", query, value);
    return value;
  }

  private static double extractSingleValue(JsonNode body, String query) {
    var status = body.path("status").asString();
    if (!"success".equals(status)) {
      var error = body.path("error").asString();
      var errorType = body.path("errorType").asString();

      throw new IllegalStateException(
          "Prometheus query failed: query="
              + query
              + ", status="
              + status
              + ", errorType="
              + errorType
              + ", error="
              + error);
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
