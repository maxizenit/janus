package org.janus.adminui.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.janus.adminui.model.OverrideStateCommand;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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

    IntegerField ttlAmount = new IntegerField("TTL value");
    ttlAmount.setMin(1);

    Select<TtlUnit> ttlUnit = new Select<>();
    ttlUnit.setLabel("TTL unit");
    ttlUnit.setItems(TtlUnit.availableFor(maxOverrideTtl));
    ttlUnit.setItemLabelGenerator(TtlUnit::label);
    ttlUnit.addValueChangeListener(
        e -> applyTtlBounds(ttlAmount, e.getValue(), maxOverrideTtl));
    ttlUnit.setValue(TtlUnit.defaultFor(maxOverrideTtl));
    applyTtlBounds(ttlAmount, ttlUnit.getValue(), maxOverrideTtl);

    Button apply =
        new Button(
            "Apply",
            e -> {
              var result =
                  validate(
                      degradationId,
                      value.getValue(),
                      ttlAmount.getValue(),
                      ttlUnit.getValue(),
                      maxOverrideTtl);
              if (result.errorMessage() != null) {
                Notification.show(result.errorMessage());
                return;
              }
              onApply.accept(result.command());
              close();
            });

    Button cancel = new Button("Cancel", e -> close());

    add(new FormLayout(id, value, ttlAmount, ttlUnit), apply, cancel);
  }

  static ValidationResult validate(
      String degradationId,
      @Nullable Double stateValue,
      @Nullable Integer ttlValue,
      @Nullable TtlUnit unit,
      Duration maxOverrideTtl) {
    if (stateValue == null || stateValue < 0.0 || stateValue > 1.0) {
      return ValidationResult.error("Value must be in range [0.0, 1.0]");
    }
    if (ttlValue == null || ttlValue < 1) {
      return ValidationResult.error("TTL must be positive");
    }
    if (unit == null) {
      return ValidationResult.error("TTL unit is required");
    }
    Duration ttl = unit.toDuration(ttlValue);
    if (ttl.compareTo(maxOverrideTtl) > 0) {
      return ValidationResult.error(
          "TTL must not exceed " + TtlUnit.formatDuration(maxOverrideTtl));
    }
    return ValidationResult.ok(new OverrideStateCommand(degradationId, stateValue, ttl));
  }

  record ValidationResult(@Nullable OverrideStateCommand command, @Nullable String errorMessage) {

    static ValidationResult ok(OverrideStateCommand command) {
      return new ValidationResult(command, null);
    }

    static ValidationResult error(String message) {
      return new ValidationResult(null, message);
    }
  }

  private static void applyTtlBounds(
      IntegerField ttlAmount, TtlUnit unit, Duration maxOverrideTtl) {
    if (unit == null) {
      return;
    }
    int maxAmount = unit.maxAmount(maxOverrideTtl);
    ttlAmount.setMax(maxAmount);
    Integer currentValue = ttlAmount.getValue();
    if (currentValue == null || currentValue > maxAmount) {
      ttlAmount.setValue(Math.min(10, maxAmount));
    }
  }

  enum TtlUnit {
    SECONDS("seconds") {
      @Override
      Duration toDuration(int value) {
        return Duration.ofSeconds(value);
      }

      @Override
      long maxAmountLong(Duration max) {
        return max.toSeconds();
      }
    },
    MINUTES("minutes") {
      @Override
      Duration toDuration(int value) {
        return Duration.ofMinutes(value);
      }

      @Override
      long maxAmountLong(Duration max) {
        return max.toMinutes();
      }
    },
    HOURS("hours") {
      @Override
      Duration toDuration(int value) {
        return Duration.ofHours(value);
      }

      @Override
      long maxAmountLong(Duration max) {
        return max.toHours();
      }
    };

    private final String label;

    TtlUnit(String label) {
      this.label = label;
    }

    String label() {
      return label;
    }

    abstract Duration toDuration(int value);

    abstract long maxAmountLong(Duration max);

    int maxAmount(Duration max) {
      return Math.toIntExact(maxAmountLong(max));
    }

    static List<TtlUnit> availableFor(Duration max) {
      var units = new ArrayList<TtlUnit>();
      for (TtlUnit unit : values()) {
        if (unit.maxAmountLong(max) >= 1) {
          units.add(unit);
        }
      }
      return List.copyOf(units);
    }

    static TtlUnit defaultFor(Duration max) {
      return max.toMinutes() >= 1 ? MINUTES : SECONDS;
    }

    static String formatDuration(Duration duration) {
      if (duration.toHours() >= 1
          && duration.toMinutesPart() == 0
          && duration.toSecondsPart() == 0) {
        return duration.toHours() + " hours";
      }
      if (duration.toMinutes() >= 1 && duration.toSecondsPart() == 0) {
        return duration.toMinutes() + " minutes";
      }
      return duration.toSeconds() + " seconds";
    }
  }
}
