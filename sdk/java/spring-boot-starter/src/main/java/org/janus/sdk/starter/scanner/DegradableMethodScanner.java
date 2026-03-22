package org.janus.sdk.starter.scanner;

import lombok.RequiredArgsConstructor;
import org.janus.sdk.annotation.Degradable;
import org.janus.sdk.core.registry.DegradableMethodRegistry;
import org.janus.sdk.core.validation.DegradableDescriptorValidator;
import org.jspecify.annotations.NullMarked;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Component
@RequiredArgsConstructor
@NullMarked
public class DegradableMethodScanner {

  private final ListableBeanFactory beanFactory;
  private final DegradableMethodRegistry registry;
  private final DegradableDescriptorFactory descriptorFactory;
  private final DegradableDescriptorValidator validator;

  public void scanAndRegister() {
    for (var beanName : beanFactory.getBeanDefinitionNames()) {
      var bean = beanFactory.getBean(beanName);
      var targetClass = AopProxyUtils.ultimateTargetClass(bean);

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
