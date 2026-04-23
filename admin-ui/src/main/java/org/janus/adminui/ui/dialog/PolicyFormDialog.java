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
        .bind(PolicyFormData::getQuery, PolicyFormData::setQuery);
    binder
        .forField(criticalThreshold)
        .bind(PolicyFormData::getCriticalThreshold, PolicyFormData::setCriticalThreshold);
    binder
        .forField(minFallbackRatio)
        .bind(PolicyFormData::getMinFallbackRatio, PolicyFormData::setMinFallbackRatio);
    binder
        .forField(maxFallbackRatio)
        .bind(PolicyFormData::getMaxFallbackRatio, PolicyFormData::setMaxFallbackRatio);
    binder
        .forField(fallbackCurveExponent)
        .bind(PolicyFormData::getFallbackCurveExponent, PolicyFormData::setFallbackCurveExponent);

    binder.readBean(data);

    Runnable updateVisibility =
        () -> {
          boolean prometheus = SignalSourceTypeView.PROMETHEUS.equals(signalSourceType.getValue());
          query.setVisible(prometheus);
        };
    signalSourceType.addValueChangeListener(e -> updateVisibility.run());
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
