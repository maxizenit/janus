package org.janus.demo.client.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.janus.sdk.core.runtime.DegradationStateRegistry;
import org.jspecify.annotations.NullMarked;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@NullMarked
public class DebugController {

  private final DegradationStateRegistry registry;

  @GetMapping("/degradations")
  public Map<String, Object> degradations() {
    return Map.of(
        "count", registry.getAll().size(),
        "states", registry.getAll());
  }
}
