package org.janus.sdk.starter.aop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class ReflectionFallbackMethodInvoker implements FallbackMethodInvoker {

  @Override
  public Object invoke(Object target, Method fallbackMethod, Object[] arguments) throws Throwable {
    try {
      fallbackMethod.setAccessible(true);
      return fallbackMethod.invoke(target, arguments);
    } catch (InvocationTargetException e) {
      throw e.getTargetException();
    }
  }
}
