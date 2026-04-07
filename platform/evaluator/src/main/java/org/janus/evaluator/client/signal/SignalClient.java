package org.janus.evaluator.client.signal;

import org.janus.evaluator.model.snapshot.SignalSourceSnapshot;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SignalClient {

  double getSignalValue(SignalSourceSnapshot signalSource);
}
