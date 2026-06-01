package org.janus.demo.client.service;

import java.util.List;
import org.janus.demo.client.dto.Track;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class RecommendationFallback {

  // The fallback chart returns the SAME number of tracks as a live personalized
  // response (10) so that degradation means substituting personalized picks with
  // a generic chart, not truncating the result. This lets the experiment measure
  // quality simply as the share of fallback (vs. live) responses. Chart track ids
  // are 1..10; live personalized ids are >=101 — the controller uses this range
  // to flag a response as degraded.
  private static final List<Track> TOP_CHART =
      List.of(
          new Track(1, "Queen", "Bohemian Rhapsody"),
          new Track(2, "The Beatles", "Hey Jude"),
          new Track(3, "Nirvana", "Smells Like Teen Spirit"),
          new Track(4, "Michael Jackson", "Billie Jean"),
          new Track(5, "Led Zeppelin", "Stairway to Heaven"),
          new Track(6, "Eagles", "Hotel California"),
          new Track(7, "AC/DC", "Back in Black"),
          new Track(8, "Guns N' Roses", "Sweet Child o' Mine"),
          new Track(9, "Pink Floyd", "Another Brick in the Wall"),
          new Track(10, "The Rolling Stones", "Paint It Black"));

  public List<Track> topChart(long userId) {
    return TOP_CHART;
  }
}
