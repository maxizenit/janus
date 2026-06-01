package org.janus.demo.client.controller;

import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.janus.demo.client.service.RecommendationService;
import org.jspecify.annotations.NullMarked;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Validated
@NullMarked
public class RecommendationController {

  private final RecommendationService service;

  @GetMapping
  public Map<String, Object> getRecommendations(@RequestParam @PositiveOrZero long userId) {

    var result = service.getRecommendations(userId);

    // A degraded (fallback) response is the generic chart whose track ids are
    // 1..10; a live personalized response has ids >=101. The flag lets the load
    // harness measure quality as the share of non-degraded responses, uniformly
    // across all configurations (base / r4j / react / proact).
    boolean degraded = !result.isEmpty() && result.get(0).id() < 100;

    return Map.of(
        "userId", userId,
        "count", result.size(),
        "degraded", degraded,
        "recommendations", result);
  }
}
