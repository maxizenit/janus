package org.janus.demo.server.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.janus.demo.server.service.DemoScenarioService;
import org.jspecify.annotations.NullMarked;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
@Validated
@NullMarked
public class DemoScenarioController {

  private final DemoScenarioService scenarioService;

  @GetMapping("/recommendations")
  public Map<String, Object> recommendations(
      @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit) throws InterruptedException {

    scenarioService.applyCurrentMode();

    var recommendations =
        Stream.of(
                "popular-1",
                "popular-2",
                "popular-3",
                "popular-4",
                "popular-5",
                "popular-6",
                "popular-7",
                "popular-8",
                "popular-9",
                "popular-10",
                "popular-11",
                "popular-12",
                "popular-13",
                "popular-14",
                "popular-15",
                "popular-16",
                "popular-17",
                "popular-18",
                "popular-19",
                "popular-20")
            .limit(limit)
            .toList();

    return Map.of(
        "mode", scenarioService.snapshot().mode(),
        "limit", limit,
        "recommendations", recommendations);
  }

  @PostMapping("/mode")
  public DemoScenarioService.ModeSnapshot changeMode(
      @RequestParam DemoScenarioService.Mode mode,
      @RequestParam(defaultValue = "0") @PositiveOrZero long delayMs,
      @RequestParam(defaultValue = "500") int status,
      @RequestParam(defaultValue = "0.3") double errorRate) {

    scenarioService.update(mode, delayMs, status, errorRate);
    return scenarioService.snapshot();
  }

  @PostMapping("/mode/ok")
  public DemoScenarioService.ModeSnapshot ok() {
    scenarioService.update(DemoScenarioService.Mode.OK, 0, 200, 0.0);
    return scenarioService.snapshot();
  }

  @PostMapping("/mode/slow")
  public DemoScenarioService.ModeSnapshot slow(
      @RequestParam(defaultValue = "3000") @PositiveOrZero long delayMs) {

    scenarioService.update(DemoScenarioService.Mode.SLOW, delayMs, 200, 0.0);
    return scenarioService.snapshot();
  }

  @PostMapping("/mode/error")
  public DemoScenarioService.ModeSnapshot error(@RequestParam(defaultValue = "500") int status) {

    scenarioService.update(DemoScenarioService.Mode.ERROR, 0, status, 1.0);
    return scenarioService.snapshot();
  }

  @GetMapping("/mode")
  public DemoScenarioService.ModeSnapshot mode() {
    return scenarioService.snapshot();
  }
}
