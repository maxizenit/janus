package org.janus.adminui.ui.dialog;

import static org.mockito.Mockito.verifyNoInteractions;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import java.util.function.Consumer;
import org.janus.adminui.model.OverrideStateCommand;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StateOverrideDialogTest {

  @Test
  void apply_emptyFields_doesNotSubmitCommand() {
    @SuppressWarnings("unchecked")
    Consumer<OverrideStateCommand> onApply = Mockito.mock(Consumer.class);
    UI.setCurrent(new UI());
    var dialog = new StateOverrideDialog("deg-1", onApply);

    find(dialog, NumberField.class).clear();
    find(dialog, IntegerField.class).clear();
    findApplyButton(dialog).click();

    verifyNoInteractions(onApply);
  }

  private static Button findApplyButton(Component root) {
    return root.getChildren()
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(button -> "Apply".equals(button.getText()))
        .findFirst()
        .orElseThrow();
  }

  private static <T extends Component> T find(Component root, Class<T> type) {
    return root.getChildren()
        .flatMap(child -> java.util.stream.Stream.concat(java.util.stream.Stream.of(child), child.getChildren()))
        .filter(type::isInstance)
        .map(type::cast)
        .findFirst()
        .orElseThrow();
  }
}
