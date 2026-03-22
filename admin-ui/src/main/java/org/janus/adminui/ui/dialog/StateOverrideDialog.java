package org.janus.adminui.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import java.time.Duration;
import java.util.function.Consumer;
import org.janus.adminui.model.OverrideStateCommand;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class StateOverrideDialog extends Dialog {

  public StateOverrideDialog(String degradationId, Consumer<OverrideStateCommand> onApply) {
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
    ttlMinutes.setValue(10);

    Button apply =
        new Button(
            "Apply",
            e -> {
              onApply.accept(
                  new OverrideStateCommand(
                      degradationId,
                      value.getValue(),
                      Duration.ofMinutes(ttlMinutes.getValue().longValue())));
              close();
            });

    Button cancel = new Button("Cancel", e -> close());

    add(new FormLayout(id, value, ttlMinutes), apply, cancel);
  }
}
