package org.janus.sdk.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Degradable {

  String value();

  String fallback() default "";

  double criticalThreshold() default Double.NaN;

  double minFallbackRatio() default Double.NaN;

  double maxFallbackRatio() default Double.NaN;
}
