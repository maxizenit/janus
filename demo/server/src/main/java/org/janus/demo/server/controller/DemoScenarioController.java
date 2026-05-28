package org.janus.demo.server.controller;

import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.janus.demo.server.dto.Track;
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

  private static final List<Track> PERSONALIZED_TRACKS =
      List.of(
          new Track(101, "Tame Impala", "Let It Happen"),
          new Track(102, "Radiohead", "Weird Fishes"),
          new Track(103, "Aphex Twin", "Avril 14th"),
          new Track(104, "Boards of Canada", "Roygbiv"),
          new Track(105, "Bonobo", "Kerala"),
          new Track(106, "Burial", "Archangel"),
          new Track(107, "Four Tet", "Two Thousand and Seventeen"),
          new Track(108, "Caribou", "Odessa"),
          new Track(109, "Floating Points", "Last Bloom"),
          new Track(110, "Nils Frahm", "Says"));

  private final DemoScenarioService scenarioService;

  @GetMapping("/recommendations")
  public Map<String, Object> recommendations(@RequestParam @PositiveOrZero long userId)
      throws InterruptedException {

    scenarioService.applyCurrentMode();

    return Map.of(
        "mode", scenarioService.snapshot().mode(),
        "userId", userId,
        "recommendations", PERSONALIZED_TRACKS);
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
