package org.janus.sdk.starter.aop;

import java.lang.reflect.Method;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface FallbackMethodInvoker {

  Object invoke(Object target, Method fallbackMethod, Object[] arguments) throws Throwable;
}
