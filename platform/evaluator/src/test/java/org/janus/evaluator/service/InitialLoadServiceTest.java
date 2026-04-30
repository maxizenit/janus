package org.janus.evaluator.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InitialLoadServiceTest {

  @Mock private PolicyRefreshService policyRefreshService;

  private InitialLoadService initialLoadService;

  @BeforeEach
  void setUp() {
    initialLoadService = new InitialLoadService(policyRefreshService);
  }

  @Test
  void onApplicationReady_swallowsInitialRefreshFailure() {
    doThrow(new RuntimeException("policy store unavailable"))
        .when(policyRefreshService)
        .refreshAllPolicies();

    assertThatCode(() -> initialLoadService.onApplicationReady()).doesNotThrowAnyException();

    verify(policyRefreshService).refreshAllPolicies();
  }
}
