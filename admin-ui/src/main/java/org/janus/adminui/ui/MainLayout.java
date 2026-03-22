package org.janus.adminui.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import org.janus.adminui.ui.view.PoliciesView;
import org.janus.adminui.ui.view.StatesView;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class MainLayout extends AppLayout {

  public MainLayout() {
    H2 title = new H2("Janus Admin UI");

    RouterLink policies = new RouterLink("Policies", PoliciesView.class);
    RouterLink states = new RouterLink("States", StatesView.class);

    VerticalLayout drawer = new VerticalLayout(title, policies, states);
    drawer.setPadding(true);
    drawer.setSpacing(true);

    addToDrawer(drawer);
  }
}
