package org.janus.adminui.ui.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import org.janus.adminui.model.PolicyView;
import org.janus.adminui.service.PolicyAdminService;
import org.janus.adminui.ui.MainLayout;
import org.janus.adminui.ui.dialog.PolicyFormDialog;
import org.jspecify.annotations.NullMarked;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Policies")
@Slf4j
@NullMarked
public class PoliciesView extends VerticalLayout {

  private final PolicyAdminService service;
  private final Grid<PolicyView> grid = new Grid<>(PolicyView.class, false);

  public PoliciesView(PolicyAdminService service) {
    this.service = service;

    Button create = new Button("Create", e -> openCreateDialog());
    Button refresh = new Button("Refresh", e -> refresh());

    grid.addColumn(PolicyView::degradationId).setHeader("Degradation ID").setAutoWidth(true);
    grid.addColumn(p -> p.evaluationInterval().toSeconds()).setHeader("Evaluation interval, sec");
    grid.addColumn(PolicyView::signalSourceType).setHeader("Source type");
    grid.addColumn(PolicyView::metricReference).setHeader("Metric reference");
    grid.addColumn(PolicyView::criticalThreshold).setHeader("Critical threshold");
    grid.addColumn(PolicyView::minFallbackRatio).setHeader("Min fallback ratio");
    grid.addColumn(PolicyView::maxFallbackRatio).setHeader("Max fallback ratio");

    grid.addComponentColumn(
            policy -> {
              Button edit = new Button("Edit", e -> openEditDialog(policy));
              Button delete = new Button("Delete", e -> confirmDelete(policy));
              return new HorizontalLayout(edit, delete);
            })
        .setHeader("Actions");

    add(new HorizontalLayout(create, refresh), grid);
    setSizeFull();
    grid.setSizeFull();

    refresh();
  }

  private void openCreateDialog() {
    new PolicyFormDialog(
            null,
            policy -> {
              service.createPolicy(policy);
              Notification.show("Policy created");
              refresh();
            })
        .open();
  }

  private void openEditDialog(PolicyView policy) {
    new PolicyFormDialog(
            policy,
            updated -> {
              service.updatePolicy(updated);
              Notification.show("Policy updated");
              refresh();
            })
        .open();
  }

  private void confirmDelete(PolicyView policy) {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Delete policy");
    dialog.setText("Delete policy " + policy.degradationId() + "?");
    dialog.setConfirmText("Delete");
    dialog.addConfirmListener(
        e -> {
          service.deletePolicy(policy.degradationId());
          Notification.show("Policy deleted");
          refresh();
        });
    dialog.open();
  }

  private void refresh() {
    log.debug("Refreshing policies grid");
    grid.setItems(service.getPolicies());
  }
}
