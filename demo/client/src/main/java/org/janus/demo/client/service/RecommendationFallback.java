package org.janus.demo.client.service;

import java.util.List;
import org.janus.demo.client.dto.Track;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class RecommendationFallback {

  private static final List<Track> TOP_CHART =
      List.of(
          new Track(1, "Queen", "Bohemian Rhapsody"),
          new Track(2, "The Beatles", "Hey Jude"),
          new Track(3, "Nirvana", "Smells Like Teen Spirit"),
          new Track(4, "Michael Jackson", "Billie Jean"),
          new Track(5, "Led Zeppelin", "Stairway to Heaven"));

  public List<Track> topChart(long userId) {
    return TOP_CHART;
  }
}
