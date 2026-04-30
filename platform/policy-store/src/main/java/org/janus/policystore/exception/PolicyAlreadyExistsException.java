package org.janus.policystore.exception;

public class PolicyAlreadyExistsException extends RuntimeException {

  public PolicyAlreadyExistsException(String degradationId) {
    super("Policy already exists: " + degradationId);
  }
}
