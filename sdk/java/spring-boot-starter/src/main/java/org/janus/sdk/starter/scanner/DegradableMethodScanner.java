package org.janus.sdk.starter.scanner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.sdk.annotation.Degradable;
import org.janus.sdk.core.registry.DegradableMethodRegistry;
import org.janus.sdk.core.validation.DegradableDescriptorValidator;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class DegradableMethodScanner {

  private final ConfigurableListableBeanFactory beanFactory;
  private final DegradableMethodRegistry registry;
  private final DegradableDescriptorFactory descriptorFactory;
  private final DegradableDescriptorValidator validator;

  public void scanAndRegister() {
    for (var beanName : beanFactory.getBeanDefinitionNames()) {
      var rawType = beanFactory.getType(beanName, false);
      if (rawType == null) {
        log.debug(
            "Skipping degradable scan: bean type unresolved without instantiation, beanName={}",
            beanName);
        continue;
      }

      var targetClass = ClassUtils.getUserClass(rawType);

      ReflectionUtils.doWithMethods(
          targetClass,
          method -> {
            var annotation = AnnotatedElementUtils.findMergedAnnotation(method, Degradable.class);
            if (annotation == null) {
              return;
            }

            var descriptor = descriptorFactory.create(targetClass, method);
            validator.validate(descriptor);
            registry.register(descriptor);
          });
    }
  }
}
