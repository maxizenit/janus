package org.janus.demo.client.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
  public Map<String, Object> getRecommendations(
      @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit) {

    var result = service.getRecommendations(limit);

    return Map.of(
        "limit", limit,
        "count", result.size(),
        "recommendations", result);
  }
}
