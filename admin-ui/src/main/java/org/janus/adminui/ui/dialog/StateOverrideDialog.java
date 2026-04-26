package org.janus.adminui.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import java.time.Duration;
import java.util.function.Consumer;
import org.janus.adminui.model.OverrideStateCommand;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class StateOverrideDialog extends Dialog {

  public StateOverrideDialog(
      String degradationId, Duration maxOverrideTtl, Consumer<OverrideStateCommand> onApply) {
    TextField id = new TextField("Degradation ID");
    id.setValue(degradationId);
    id.setReadOnly(true);

    NumberField value = new NumberField("Value");
    value.setMin(0.0);
    value.setMax(1.0);
    value.setStep(0.01);
    value.setValue(1.0);

    IntegerField ttlMinutes = new IntegerField("TTL, minutes");
    ttlMinutes.setMin(1);
    long maxOverrideTtlMinutes = maxOverrideTtl.toMinutes();
    int defaultTtlMinutes = 10;
    if (maxOverrideTtlMinutes >= 1) {
      int maxTtlMinutes = Math.toIntExact(maxOverrideTtlMinutes);
      ttlMinutes.setMax(maxTtlMinutes);
      defaultTtlMinutes = Math.min(defaultTtlMinutes, maxTtlMinutes);
    }
    ttlMinutes.setValue(defaultTtlMinutes);

    Button apply =
        new Button(
            "Apply",
            e -> {
              var stateValue = value.getValue();
              var ttlValue = ttlMinutes.getValue();
              if (stateValue == null || stateValue < 0.0 || stateValue > 1.0) {
                Notification.show("Value must be in range [0.0, 1.0]");
                return;
              }
              if (ttlValue == null || ttlValue < 1) {
                Notification.show("TTL must be positive");
                return;
              }
              Duration ttl = Duration.ofMinutes(ttlValue.longValue());
              if (ttl.compareTo(maxOverrideTtl) > 0) {
                Notification.show("TTL must not exceed " + maxOverrideTtl.toMinutes() + " minutes");
                return;
              }

              onApply.accept(
                  new OverrideStateCommand(degradationId, stateValue, ttl));
              close();
            });

    Button cancel = new Button("Cancel", e -> close());

    add(new FormLayout(id, value, ttlMinutes), apply, cancel);
  }
}
