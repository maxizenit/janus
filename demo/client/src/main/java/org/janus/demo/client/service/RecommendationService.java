package org.janus.demo.client.service;

import java.util.List;
import org.janus.demo.client.dto.Track;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface RecommendationService {

  List<Track> getRecommendations(long userId);
}
