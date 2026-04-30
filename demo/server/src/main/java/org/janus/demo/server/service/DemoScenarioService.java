package org.janus.demo.server.service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DemoScenarioService {

  private final AtomicReference<ModeSnapshot> snapshot =
      new AtomicReference<>(new ModeSnapshot(Mode.OK, 0, 200, 0.0));

  public void update(Mode mode, long delayMs, int status, double errorRate) {
    snapshot.set(new ModeSnapshot(mode, delayMs, status, errorRate));
  }

  public ModeSnapshot snapshot() {
    return snapshot.get();
  }

  public void applyCurrentMode() throws InterruptedException {
    var current = snapshot();

    switch (current.mode()) {
      case OK -> {}
      case SLOW -> sleep(current.delayMs());
      case ERROR -> throw error(current.status());
      case FLAKY -> {
        sleep(current.delayMs());
        if (ThreadLocalRandom.current().nextDouble() < current.errorRate()) {
          throw error(current.status());
        }
      }
    }
  }

  private void sleep(long delayMs) throws InterruptedException {
    if (delayMs > 0) {
      Thread.sleep(delayMs);
    }
  }

  private ResponseStatusException error(int status) {
    return new ResponseStatusException(
        HttpStatus.valueOf(status), "Demo server forced error: " + status);
  }

  public enum Mode {
    OK,
    SLOW,
    ERROR,
    FLAKY
  }

  public record ModeSnapshot(Mode mode, long delayMs, int status, double errorRate) {}
}
