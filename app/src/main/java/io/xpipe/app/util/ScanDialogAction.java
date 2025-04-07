package io.xpipe.app.util;

import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellTtyState;

import javafx.collections.ObservableList;

public interface ScanDialogAction {

    static ScanDialogAction shellScanAction() {
        var action = new ScanDialogAction() {

            @Override
            public boolean scan(
                    ObservableList<ScanProvider.ScanOpportunity> all,
                    ObservableList<ScanProvider.ScanOpportunity> selected,
                    DataStoreEntry entry,
                    ShellControl sc) {
                if (!sc.canHaveSubshells()) {
                    return false;
                }

                if (!sc.getShellDialect().getDumbMode().supportsAnyPossibleInteraction()) {
                    return false;
                }

                if (sc.getTtyState() != ShellTtyState.NONE) {
                    return false;
                }

                var providers = ScanProvider.getAll();
                for (ScanProvider scanProvider : providers) {
                    try {
                        // Previous scan operation could have exited the shell
                        sc.start();
                        ScanProvider.ScanOpportunity operation = scanProvider.create(entry, sc);
                        if (operation != null) {
                            if (!operation.isDisabled() && operation.isDefaultSelected()) {
                                selected.add(operation);
                            }
                            all.add(operation);
                        }
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).handle();
                    }
                }
                return true;
            }
        };
        return action;
    }

    boolean scan(
            ObservableList<ScanProvider.ScanOpportunity> all,
            ObservableList<ScanProvider.ScanOpportunity> selected,
            DataStoreEntry entry,
            ShellControl shellControl);
}
