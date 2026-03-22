package org.janus.adminui.ui.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;
import org.janus.adminui.configuration.properties.AdminUiProperties;
import org.janus.adminui.model.SourceStateView;
import org.janus.adminui.model.StateView;
import org.janus.adminui.service.StateAdminService;
import org.janus.adminui.ui.MainLayout;
import org.janus.adminui.ui.dialog.StateOverrideDialog;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Route(value = "states", layout = MainLayout.class)
@PageTitle("States")
@Slf4j
@NullMarked
public class StatesView extends VerticalLayout {

  private final StateAdminService service;
  private final AdminUiProperties properties;
  private final Grid<StateView> grid = new Grid<>(StateView.class, false);

  @Nullable private Registration pollRegistration;

  public StatesView(StateAdminService service, AdminUiProperties properties) {
    this.service = service;
    this.properties = properties;

    Button refresh = new Button("Refresh", e -> refresh());

    grid.addColumn(StateView::degradationId).setHeader("Degradation ID").setAutoWidth(true);
    grid.addColumn(StateView::effectiveValue).setHeader("Effective value");
    grid.addColumn(StateView::effectiveSource).setHeader("Effective source");
    grid.addColumn(StatesView::formatSources).setHeader("Source states");
    grid.addColumn(StateView::refreshedAt).setHeader("Refreshed at");

    grid.addComponentColumn(
            state -> {
              Button override = new Button("Override", e -> openOverrideDialog(state));
              Button clear =
                  new Button(
                      "Clear override",
                      e -> {
                        service.clearOverride(state.degradationId());
                        Notification.show("Override cleared");
                        refresh();
                      });
              return new HorizontalLayout(override, clear);
            })
        .setHeader("Actions");

    add(new HorizontalLayout(refresh), grid);
    setSizeFull();
    grid.setSizeFull();
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    UI ui = attachEvent.getUI();
    int intervalMs = Math.toIntExact(properties.stateRefreshInterval().toMillis());
    ui.setPollInterval(intervalMs);
    pollRegistration = ui.addPollListener(event -> refresh());

    log.info("States view attached: pollIntervalMs={}", intervalMs);
    refresh();
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    if (pollRegistration != null) {
      pollRegistration.remove();
      pollRegistration = null;
    }
    detachEvent.getUI().setPollInterval(-1);
    log.info("States view detached");
  }

  private void openOverrideDialog(StateView state) {
    new StateOverrideDialog(
            state.degradationId(),
            command -> {
              service.applyOverride(command);
              Notification.show("Override applied");
              refresh();
            })
        .open();
  }

  private void refresh() {
    log.debug("Refreshing states grid");
    grid.setItems(service.getStatesForAllPolicies());
  }

  private static String formatSources(StateView state) {
    return state.sourceStates().stream()
        .map(StatesView::formatSource)
        .reduce((left, right) -> left + " | " + right)
        .orElse("");
  }

  private static String formatSource(SourceStateView source) {
    return source.source()
        + "="
        + source.value()
        + " (ttl="
        + source.remainingTtl().toSeconds()
        + "s)";
  }
}
