package org.janus.sdk.starter.aop;

import java.util.ArrayDeque;
import java.util.Deque;
import org.jspecify.annotations.NullMarked;

/**
 * Thread-local stack of degradation ids tracking which {@code @Degradable} method initiated the
 * current invocation chain. Used by {@link DegradableAspect} to tag metrics with {@code
 * caller.degradation.id} so cascaded fallback events are distinguishable from top-level ones.
 *
 * <p>The stack is purely synchronous: it is not propagated across {@code @Async}, reactive
 * pipelines, or manually spawned threads. For asynchronous flows the caller will appear as {@link
 * #NONE} on the downstream thread.
 */
@NullMarked
public final class FallbackCallerContext {

  public static final String NONE = "none";

  private static final ThreadLocal<Deque<String>> STACK = new ThreadLocal<>();

  private FallbackCallerContext() {}

  public static String currentOrNone() {
    var stack = STACK.get();
    if (stack == null || stack.isEmpty()) {
      return NONE;
    }
    return stack.peek();
  }

  public static void push(String degradationId) {
    var stack = STACK.get();
    if (stack == null) {
      stack = new ArrayDeque<>();
      STACK.set(stack);
    }
    stack.push(degradationId);
  }

  public static void pop() {
    var stack = STACK.get();
    if (stack == null) {
      return;
    }
    if (!stack.isEmpty()) {
      stack.pop();
    }
    if (stack.isEmpty()) {
      STACK.remove();
    }
  }
}
