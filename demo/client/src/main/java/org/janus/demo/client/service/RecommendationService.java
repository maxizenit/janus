package org.janus.demo.client.service;

import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.janus.demo.client.integration.DemoServerClient;
import org.janus.sdk.annotation.Degradable;
import org.janus.sdk.annotation.param.Bounds;
import org.janus.sdk.annotation.param.RelativeScale;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@NullMarked
public class RecommendationService {

  private final DemoServerClient demoServerClient;

  @Degradable(
      value = "recommendations.fetch",
      fallback = "getRecommendationsFallback",
      criticalThreshold = 0.7,
      minFallbackRatio = 0.3,
      maxFallbackRatio = 1.0)
  public List<String> getRecommendations(
      @RelativeScale(minFactor = 0.2, maxFactor = 1.0) @Bounds(min = 1, max = 20) int limit) {

    return demoServerClient.fetchRecommendations(limit);
  }

  public List<String> getRecommendationsFallback(int limit) {
    return Stream.of("fallback-1", "fallback-2", "fallback-3", "fallback-4", "fallback-5")
        .limit(limit)
        .toList();
  }
}
