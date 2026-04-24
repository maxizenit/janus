package org.janus.adminui.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.time.Duration;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import org.janus.adminui.model.PolicyView;
import org.janus.adminui.model.SignalSourceTypeView;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PolicyFormDialog extends Dialog {

  private final Binder<PolicyFormData> binder = new Binder<>(PolicyFormData.class);

  public PolicyFormDialog(@Nullable PolicyView existing, Consumer<PolicyView> onSave) {
    PolicyFormData data = PolicyFormData.from(existing);

    TextField degradationId = new TextField("Degradation ID");
    IntegerField evaluationIntervalSeconds = new IntegerField("Evaluation interval, sec");
    evaluationIntervalSeconds.setMin(1);
    Select<SignalSourceTypeView> signalSourceType = new Select<>();
    signalSourceType.setLabel("Signal source type");
    signalSourceType.setItems(SignalSourceTypeView.values());

    TextArea query = new TextArea("Prometheus query");
    query.setMinHeight("100px");
    query.setWidthFull();
    query.setPlaceholder("e.g. rate(http_errors_total[5m]) / rate(http_requests_total[5m])");

    NumberField criticalThreshold = new NumberField("Critical threshold");
    NumberField minFallbackRatio = new NumberField("Min fallback ratio");
    NumberField maxFallbackRatio = new NumberField("Max fallback ratio");
    NumberField fallbackCurveExponent = new NumberField("Fallback curve exponent");

    binder
        .forField(degradationId)
        .asRequired()
        .bind(PolicyFormData::getDegradationId, PolicyFormData::setDegradationId);
    binder
        .forField(evaluationIntervalSeconds)
        .asRequired()
        .withValidator(value -> value != null && value > 0, "Evaluation interval must be positive")
        .bind(
            PolicyFormData::getEvaluationIntervalSeconds,
            PolicyFormData::setEvaluationIntervalSeconds);
    binder
        .forField(signalSourceType)
        .asRequired()
        .bind(PolicyFormData::getSignalSourceType, PolicyFormData::setSignalSourceType);
    binder
        .forField(query)
        .withValidator(
            value ->
                !SignalSourceTypeView.PROMETHEUS.equals(signalSourceType.getValue())
                    || hasText(value),
            "Prometheus query is required")
        .bind(PolicyFormData::getQuery, PolicyFormData::setQuery);
    binder
        .forField(criticalThreshold)
        .withValidator(
            PolicyFormDialog::isNullOrRatio,
            "Critical threshold must be in range [0.0, 1.0]")
        .bind(PolicyFormData::getCriticalThreshold, PolicyFormData::setCriticalThreshold);
    binder
        .forField(minFallbackRatio)
        .withValidator(
            PolicyFormDialog::isNullOrRatio,
            "Min fallback ratio must be in range [0.0, 1.0]")
        .withValidator(
            value -> value == null || maxFallbackRatio.getValue() == null
                || value <= maxFallbackRatio.getValue(),
            "Min fallback ratio must be less than or equal to max fallback ratio")
        .bind(PolicyFormData::getMinFallbackRatio, PolicyFormData::setMinFallbackRatio);
    binder
        .forField(maxFallbackRatio)
        .withValidator(
            PolicyFormDialog::isNullOrRatio,
            "Max fallback ratio must be in range [0.0, 1.0]")
        .withValidator(
            value -> value == null || minFallbackRatio.getValue() == null
                || minFallbackRatio.getValue() <= value,
            "Max fallback ratio must be greater than or equal to min fallback ratio")
        .bind(PolicyFormData::getMaxFallbackRatio, PolicyFormData::setMaxFallbackRatio);
    binder
        .forField(fallbackCurveExponent)
        .withValidator(
            value -> value == null || value > 0.0,
            "Fallback curve exponent must be positive")
        .bind(PolicyFormData::getFallbackCurveExponent, PolicyFormData::setFallbackCurveExponent);

    binder.readBean(data);

    Runnable updateVisibility =
        () -> {
          boolean prometheus = SignalSourceTypeView.PROMETHEUS.equals(signalSourceType.getValue());
          query.setVisible(prometheus);
          if (!prometheus) {
            query.clear();
          }
        };
    signalSourceType.addValueChangeListener(
        e -> {
          updateVisibility.run();
          binder.validate();
        });
    minFallbackRatio.addValueChangeListener(e -> binder.validate());
    maxFallbackRatio.addValueChangeListener(e -> binder.validate());
    updateVisibility.run();

    Button save =
        new Button(
            "Save",
            e -> {
              PolicyFormData form = new PolicyFormData();
              if (binder.writeBeanIfValid(form)) {
                onSave.accept(form.toView());
                close();
              }
            });
    Button cancel = new Button("Cancel", e -> close());

    add(
        new FormLayout(
            degradationId,
            evaluationIntervalSeconds,
            signalSourceType,
            query,
            criticalThreshold,
            minFallbackRatio,
            maxFallbackRatio,
            fallbackCurveExponent),
        new HorizontalLayout(save, cancel));
  }

  private static boolean hasText(@Nullable String value) {
    return value != null && !value.isBlank();
  }

  private static boolean isNullOrRatio(@Nullable Double value) {
    return value == null || (value >= 0.0 && value <= 1.0);
  }

  @Getter
  @Setter
  public static class PolicyFormData {
    private String degradationId;
    private Integer evaluationIntervalSeconds;
    private SignalSourceTypeView signalSourceType;
    @Nullable private String query;
    @Nullable private Double criticalThreshold;
    @Nullable private Double minFallbackRatio;
    @Nullable private Double maxFallbackRatio;
    @Nullable private Double fallbackCurveExponent;

    static PolicyFormData from(@Nullable PolicyView view) {
      PolicyFormData data = new PolicyFormData();
      if (view != null) {
        data.degradationId = view.degradationId();
        data.evaluationIntervalSeconds = Math.toIntExact(view.evaluationInterval().toSeconds());
        data.signalSourceType = view.signalSourceType();
        data.query = view.query();
        data.criticalThreshold = view.criticalThreshold();
        data.minFallbackRatio = view.minFallbackRatio();
        data.maxFallbackRatio = view.maxFallbackRatio();
        data.fallbackCurveExponent = view.fallbackCurveExponent();
      } else {
        data.signalSourceType = SignalSourceTypeView.MANUAL;
      }
      return data;
    }

    PolicyView toView() {
      return new PolicyView(
          degradationId,
          Duration.ofSeconds(evaluationIntervalSeconds.longValue()),
          signalSourceType,
          query,
          criticalThreshold,
          minFallbackRatio,
          maxFallbackRatio,
          fallbackCurveExponent);
    }
  }
}
