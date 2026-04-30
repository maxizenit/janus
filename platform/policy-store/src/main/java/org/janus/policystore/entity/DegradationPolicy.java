package org.janus.policystore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class DegradationPolicy {

  @Id @NotBlank private String degradationId;

  @Column(nullable = false)
  @Positive
  private Long evaluationIntervalMs;

  @Enumerated(EnumType.STRING)
  private SignalSourceType signalSourceType;

  private String sourcePrometheusQuery;

  @Column(nullable = false)
  @NotNull
  @DecimalMin("0.0")
  @DecimalMax("1.0")
  private Double criticalThreshold;

  @Column(nullable = false)
  @NotNull
  @DecimalMin("0.0")
  @DecimalMax("1.0")
  private Double minFallbackRatio;

  @Column(nullable = false)
  @NotNull
  @DecimalMin("0.0")
  @DecimalMax("1.0")
  private Double maxFallbackRatio;

  @Column(nullable = false)
  @NotNull
  @DecimalMin(value = "0.0", inclusive = false)
  private Double fallbackCurveExponent;
}
