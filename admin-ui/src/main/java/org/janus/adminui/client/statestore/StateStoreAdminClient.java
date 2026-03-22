package org.janus.adminui.client.statestore;

import java.util.List;
import org.janus.adminui.model.OverrideStateCommand;
import org.janus.adminui.model.StateView;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface StateStoreAdminClient {

  List<StateView> getAdminStates(List<String> degradationIds);

  void applyOverride(OverrideStateCommand command);

  void clearOverride(String degradationId);
}
