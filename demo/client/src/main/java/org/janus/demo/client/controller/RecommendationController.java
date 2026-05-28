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

    return Map.of(
        "userId", userId,
        "count", result.size(),
        "recommendations", result);
  }
}
