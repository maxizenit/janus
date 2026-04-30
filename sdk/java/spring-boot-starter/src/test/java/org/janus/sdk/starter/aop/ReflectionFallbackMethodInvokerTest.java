package org.janus.sdk.starter.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReflectionFallbackMethodInvokerTest {

  private final ReflectionFallbackMethodInvoker invoker = new ReflectionFallbackMethodInvoker();

  @SuppressWarnings("unused")
  static class TargetService {

    public String publicFallback(String input, int count) {
      return input + ":" + count;
    }

    private String privateFallback(String input) {
      return "private:" + input;
    }

    public void throwingFallback() {
      throw new IllegalStateException("fallback error");
    }
  }

  @Test
  void invokesPublicFallbackWithCorrectArguments() throws Throwable {
    var target = new TargetService();
    var method = TargetService.class.getDeclaredMethod("publicFallback", String.class, int.class);

    var result = invoker.invoke(target, method, new Object[] {"hello", 42});

    assertThat(result).isEqualTo("hello:42");
  }

  @Test
  void invokesPrivateFallbackByMakingItAccessible() throws Throwable {
    var target = new TargetService();
    var method = TargetService.class.getDeclaredMethod("privateFallback", String.class);

    var result = invoker.invoke(target, method, new Object[] {"secret"});

    assertThat(result).isEqualTo("private:secret");
  }

  @Test
  void unwrapsInvocationTargetException() throws Exception {
    var target = new TargetService();
    var method = TargetService.class.getDeclaredMethod("throwingFallback");

    assertThatThrownBy(() -> invoker.invoke(target, method, new Object[] {}))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("fallback error");
  }
}
