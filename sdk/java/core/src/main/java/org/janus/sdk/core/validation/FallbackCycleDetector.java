package org.janus.sdk.core.validation;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class FallbackCycleDetector {

  public void detect(Collection<DegradableMethodDescriptor> descriptors) {
    Map<Method, DegradableMethodDescriptor> byMethod = new HashMap<>();
    for (var descriptor : descriptors) {
      byMethod.put(descriptor.method(), descriptor);
    }

    Set<Method> visited = new HashSet<>();
    Set<Method> onPath = new HashSet<>();
    Deque<Method> path = new ArrayDeque<>();

    for (var descriptor : descriptors) {
      if (!visited.contains(descriptor.method())) {
        dfs(descriptor, byMethod, visited, onPath, path);
      }
    }
  }

  private void dfs(
      DegradableMethodDescriptor descriptor,
      Map<Method, DegradableMethodDescriptor> byMethod,
      Set<Method> visited,
      Set<Method> onPath,
      Deque<Method> path) {
    Method method = descriptor.method();
    onPath.add(method);
    path.push(method);

    Method fallback = descriptor.fallbackMethod();
    if (fallback != null) {
      if (onPath.contains(fallback)) {
        throw new InvalidDegradableDefinitionException(
            "Fallback methods form a cycle: " + formatCycle(buildCycle(path, fallback)));
      }
      DegradableMethodDescriptor next = byMethod.get(fallback);
      if (next != null && !visited.contains(fallback)) {
        dfs(next, byMethod, visited, onPath, path);
      }
    }

    onPath.remove(method);
    visited.add(method);
    path.pop();
  }

  private List<Method> buildCycle(Deque<Method> path, Method cycleStart) {
    List<Method> cycle = new ArrayList<>();
    boolean found = false;
    for (var it = path.descendingIterator(); it.hasNext(); ) {
      Method m = it.next();
      if (m.equals(cycleStart)) {
        found = true;
      }
      if (found) {
        cycle.add(m);
      }
    }
    cycle.add(cycleStart);
    return cycle;
  }

  private String formatCycle(List<Method> methods) {
    return methods.stream().map(Method::toGenericString).collect(Collectors.joining(" -> "));
  }
}
