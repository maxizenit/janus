package org.janus.decider.scheduling;

import java.time.Instant;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.decider.model.scheduling.EvaluationTask;
import org.janus.decider.service.EvaluationService;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class EvaluationCoordinator implements EvaluationScheduler, SmartLifecycle {

  private final EvaluationService evaluationService;
  private final ExecutorService evaluationExecutor;

  private final DelayQueue<EvaluationTask> queue = new DelayQueue<>();
  private final AtomicBoolean running = new AtomicBoolean(false);

  @Nullable private Thread coordinatorThread;

  @Override
  public void start() {
    if (!running.compareAndSet(false, true)) {
      return;
    }

    coordinatorThread =
        Thread.ofPlatform().name("decider-evaluation-coordinator").start(this::runLoop);

    log.info("EvaluationCoordinator started");
  }

  private void runLoop() {
    while (running.get()) {
      try {
        var task = queue.take();
        evaluationExecutor.submit(
            () ->
                evaluationService
                    .evaluate(task.degradationId())
                    .ifPresent(
                        result -> scheduleAt(result.degradationId(), result.nextEvaluationAt())));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        log.error("Evaluation coordinator loop failed", e);
      }
    }
  }

  @Override
  public void stop() {
    if (!running.compareAndSet(true, false)) {
      return;
    }

    if (coordinatorThread != null) {
      coordinatorThread.interrupt();
    }

    log.info("EvaluationCoordinator stopped");
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  @Override
  public void scheduleNow(String degradationId) {
    queue.offer(EvaluationTask.immediate(degradationId));
  }

  @Override
  public void scheduleAt(String degradationId, Instant instant) {
    queue.offer(new EvaluationTask(degradationId, instant));
  }
}
