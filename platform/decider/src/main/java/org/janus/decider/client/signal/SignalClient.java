package org.janus.decider.client.signal;

import java.time.Duration;
import org.janus.decider.model.snapshot.SignalSourceSnapshot;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SignalClient {

  double getSignalValue(SignalSourceSnapshot signalSource, Duration evaluationInterval);
}
