package org.janus.adminui.ui;

import com.vaadin.flow.component.notification.Notification;
import io.grpc.StatusRuntimeException;

public final class ErrorNotifications {

  private ErrorNotifications() {}

  public static void show(String action, RuntimeException exception) {
    Notification.show(action + " failed: " + message(exception));
  }

  private static String message(RuntimeException exception) {
    if (exception instanceof StatusRuntimeException statusException) {
      String description = statusException.getStatus().getDescription();
      if (description != null && !description.isBlank()) {
        return description;
      }
      return statusException.getStatus().getCode().name();
    }

    String message = exception.getMessage();
    return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
  }
}
