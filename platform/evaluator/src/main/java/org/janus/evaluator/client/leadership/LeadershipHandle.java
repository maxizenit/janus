package org.janus.evaluator.client.leadership;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface LeadershipHandle extends AutoCloseable {

  boolean acquired();

  void release();

  @Override
  void close();
}
