package org.janus.sdk.annotation.param;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RelativeScale {

  double minFactor() default 0.0;

  double maxFactor() default 1.0;

  Direction direction() default Direction.DECREASE;

  double min() default Double.NaN;

  double max() default Double.NaN;
}
