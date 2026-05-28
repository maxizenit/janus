package org.janus.demo.client.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.janus.demo.client.dto.Track;
import org.janus.demo.client.integration.DemoServerClient;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("r4j")
@ConditionalOnProperty(name = "resilience4j.enabled", havingValue = "true")
@RequiredArgsConstructor
@NullMarked
public class Resilience4jRecommendationService implements RecommendationService {

  private final DemoServerClient demoServerClient;
  private final RecommendationFallback fallback;

  @Override
  @CircuitBreaker(name = "recommendations", fallbackMethod = "getRecommendationsFallback")
  public List<Track> getRecommendations(long userId) {
    return demoServerClient.fetchRecommendations(userId);
  }

  public List<Track> getRecommendationsFallback(long userId, Throwable t) {
    return fallback.topChart(userId);
  }
}
