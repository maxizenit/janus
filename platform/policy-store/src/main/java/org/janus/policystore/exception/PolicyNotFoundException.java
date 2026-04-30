package org.janus.policystore.exception;

public class PolicyNotFoundException extends RuntimeException {

  public PolicyNotFoundException(String degradationId) {
    super("Policy not found: " + degradationId);
  }
}
