package org.janus.sidecar.scheduling;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.sidecar.model.scheduling.StateRefreshTask;
import org.janus.sidecar.service.StateRefreshService;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class StateRefreshCoordinator implements StateRefreshScheduler, SmartLifecycle {

  private final StateRefreshService stateRefreshService;
  private final ExecutorService stateRefreshExecutor;

  private final DelayQueue<StateRefreshTask> queue = new DelayQueue<>();
  private final AtomicBoolean running = new AtomicBoolean(false);

  @Nullable private Thread coordinatorThread;

  @Override
  public void start() {
    if (!running.compareAndSet(false, true)) {
      return;
    }

    coordinatorThread =
        Thread.ofPlatform().name("sidecar-state-refresh-coordinator").start(this::runLoop);

    log.info("StateRefreshCoordinator started");
  }

  private void runLoop() {
    while (running.get()) {
      try {
        var first = queue.take();

        var batch = new ArrayList<StateRefreshTask>();
        batch.add(first);
        queue.drainTo(batch);

        var ids = batch.stream().map(StateRefreshTask::degradationId).collect(Collectors.toSet());
        log.debug("Scheduling state refresh batch: size={}", ids.size());

        stateRefreshExecutor.submit(
            () -> {
              var results = stateRefreshService.refresh(ids);
              for (var result : results) {
                scheduleAt(result.degradationId(), result.nextRefreshAt());
              }
            });
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        log.error("State refresh coordinator loop failed", e);
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

    log.info("StateRefreshCoordinator stopped");
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  @Override
  public void scheduleNow(String degradationId) {
    queue.offer(StateRefreshTask.immediate(degradationId));
  }

  @Override
  public void scheduleAt(String degradationId, Instant instant) {
    queue.offer(new StateRefreshTask(degradationId, instant));
  }
}
