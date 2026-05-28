package org.janus.demo.client.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.janus.demo.client.dto.Track;
import org.janus.demo.client.integration.DemoServerClient;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("default")
@RequiredArgsConstructor
@NullMarked
public class BaseRecommendationService implements RecommendationService {

  private final DemoServerClient demoServerClient;

  @Override
  public List<Track> getRecommendations(long userId) {
    return demoServerClient.fetchRecommendations(userId);
  }
}
