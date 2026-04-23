package org.janus.adminui.ui.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.janus.adminui.model.PolicyView;
import org.janus.adminui.model.SignalSourceTypeView;
import org.junit.jupiter.api.Test;

class PolicyFormDialogTest {

  @Test
  void policyFormData_toView_preservesIntegerSeconds() {
    var data =
        PolicyFormDialog.PolicyFormData.from(
            new PolicyView(
                "deg-1",
                Duration.ofSeconds(15),
                SignalSourceTypeView.MANUAL,
                null,
                null,
                null,
                null,
                null));

    PolicyView view = data.toView();

    assertThat(view.evaluationInterval()).isEqualTo(Duration.ofSeconds(15));
  }
}
