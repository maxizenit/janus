package org.janus.common.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

@RequiredArgsConstructor
@NullMarked
public class DefaultDeadlineInterceptor implements ClientInterceptor {

  private final Duration defaultDeadline;

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
    CallOptions effective =
        callOptions.getDeadline() == null
            ? callOptions.withDeadlineAfter(defaultDeadline.toMillis(), TimeUnit.MILLISECONDS)
            : callOptions;
    return next.newCall(method, effective);
  }
}
