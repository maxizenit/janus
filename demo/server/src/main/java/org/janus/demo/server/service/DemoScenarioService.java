package org.janus.demo.server.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DemoScenarioService {

  private final AtomicReference<ModeSnapshot> snapshot =
      new AtomicReference<>(new ModeSnapshot(Mode.OK, 0, 200, 0.0, 0));

  // Saturation modelling (Mode.SATURATE) — experiment v2, phase 3 (pre-emptive
  // degradation). The dependency owns a bounded resource of `maxConcurrent`
  // permits (e.g. a connection/worker pool). Each request acquires a permit,
  // holds it for delayMs, releases it. Under ramped load the resource saturates
  // (in-flight -> maxConcurrent) and a queue forms BEFORE per-call latency
  // visibly spikes. The Gauge `demo_saturation` (in [0,1]) exposes this internal
  // state to Prometheus: a proactive policy reads it and degrades pre-emptively,
  // whereas a circuit breaker — which only observes the per-call outcome/latency
  // — reacts only once latency has already risen. This is the signal a circuit
  // breaker structurally cannot see.
  private final AtomicInteger inFlight = new AtomicInteger(0); // permits currently held
  private final AtomicInteger waiting = new AtomicInteger(0); // requests blocked on acquire
  private volatile Semaphore semaphore = new Semaphore(1, true);
  private volatile int maxConcurrent = 1;

  public DemoScenarioService(MeterRegistry registry) {
    Gauge.builder("demo.saturation", this, DemoScenarioService::saturation)
        .description("Bounded-resource utilisation: in-flight / maxConcurrent, in [0,1]")
        .register(registry);
    Gauge.builder("demo.inflight", inFlight, AtomicInteger::get)
        .description("Requests currently holding a permit (being processed)")
        .register(registry);
    Gauge.builder("demo.queue", waiting, AtomicInteger::get)
        .description("Requests blocked waiting for a permit")
        .register(registry);
  }

  private double saturation() {
    int m = maxConcurrent;
    return m <= 0 ? 0.0 : Math.min(1.0, inFlight.get() / (double) m);
  }

  /** Backwards-compatible update for modes that do not use the bounded resource. */
  public void update(Mode mode, long delayMs, int status, double errorRate) {
    update(mode, delayMs, status, errorRate, 0);
  }

  public void update(Mode mode, long delayMs, int status, double errorRate, int maxConcurrent) {
    if (mode == Mode.SATURATE && maxConcurrent > 0 && maxConcurrent != this.maxConcurrent) {
      this.maxConcurrent = maxConcurrent;
      this.semaphore = new Semaphore(maxConcurrent, true);
    }
    snapshot.set(new ModeSnapshot(mode, delayMs, status, errorRate, maxConcurrent));
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
      case SATURATE -> saturate(current.delayMs());
    }
  }

  private void saturate(long processingMs) throws InterruptedException {
    waiting.incrementAndGet();
    semaphore.acquire();
    waiting.decrementAndGet();
    inFlight.incrementAndGet();
    try {
      sleep(processingMs);
    } finally {
      inFlight.decrementAndGet();
      semaphore.release();
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
    FLAKY,
    SATURATE
  }

  public record ModeSnapshot(
      Mode mode, long delayMs, int status, double errorRate, int maxConcurrent) {}
}
