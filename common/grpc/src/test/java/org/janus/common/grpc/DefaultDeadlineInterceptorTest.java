package org.janus.common.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Deadline;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class DefaultDeadlineInterceptorTest {

  private final MethodDescriptor<String, String> methodDescriptor =
      MethodDescriptor.<String, String>newBuilder()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName("svc/method")
          .setRequestMarshaller(new StubMarshaller())
          .setResponseMarshaller(new StubMarshaller())
          .build();

  @Test
  void setsDefaultDeadlineWhenNonePresent() {
    var interceptor = new DefaultDeadlineInterceptor(Duration.ofSeconds(5));
    var nextChannel = new CapturingChannel();

    interceptor.interceptCall(methodDescriptor, CallOptions.DEFAULT, nextChannel);

    assertThat(nextChannel.captured.getDeadline()).isNotNull();
    long remainingMs = nextChannel.captured.getDeadline().timeRemaining(TimeUnit.MILLISECONDS);
    assertThat(remainingMs).isBetween(4_000L, 5_000L);
  }

  @Test
  void preservesExplicitDeadline() {
    var interceptor = new DefaultDeadlineInterceptor(Duration.ofSeconds(5));
    var nextChannel = new CapturingChannel();
    var explicit = Deadline.after(500, TimeUnit.MILLISECONDS);
    var explicitOptions = CallOptions.DEFAULT.withDeadline(explicit);

    interceptor.interceptCall(methodDescriptor, explicitOptions, nextChannel);

    assertThat(nextChannel.captured.getDeadline()).isSameAs(explicit);
  }

  private static final class CapturingChannel extends Channel {
    CallOptions captured;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(
        MethodDescriptor<ReqT, RespT> method, CallOptions callOptions) {
      this.captured = callOptions;
      return null;
    }

    @Override
    public String authority() {
      return "test";
    }
  }

  private static final class StubMarshaller implements MethodDescriptor.Marshaller<String> {
    @Override
    public java.io.InputStream stream(String value) {
      throw Status.INTERNAL.withDescription("not used").asRuntimeException();
    }

    @Override
    public String parse(java.io.InputStream stream) {
      throw Status.INTERNAL.withDescription("not used").asRuntimeException();
    }
  }
}
