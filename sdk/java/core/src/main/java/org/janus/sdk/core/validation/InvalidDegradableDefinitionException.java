package org.janus.sdk.core.validation;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class InvalidDegradableDefinitionException extends RuntimeException {

  public InvalidDegradableDefinitionException(String message) {
    super(message);
  }
}
