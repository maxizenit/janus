package org.janus.demo.client.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.janus.demo.client.dto.Track;
import org.janus.demo.client.integration.DemoServerClient;
import org.janus.sdk.annotation.Degradable;
import org.janus.sdk.starter.configuration.ConditionalOnJanusSdkEnabled;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Service
@Profile("janus")
@ConditionalOnJanusSdkEnabled
@RequiredArgsConstructor
@NullMarked
public class JanusRecommendationService implements RecommendationService {

  private final DemoServerClient demoServerClient;
  private final RecommendationFallback fallback;

  @Override
  @Degradable(
      value = "recommendations.fetch",
      fallback = "getRecommendationsFallback",
      fallbackOnException = {ResourceAccessException.class, RestClientException.class})
  public List<Track> getRecommendations(long userId) {
    return demoServerClient.fetchRecommendations(userId);
  }

  public List<Track> getRecommendationsFallback(long userId) {
    return fallback.topChart(userId);
  }
}
