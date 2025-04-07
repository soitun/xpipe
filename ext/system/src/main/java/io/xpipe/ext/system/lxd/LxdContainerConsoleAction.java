package io.xpipe.ext.system.lxd;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.ext.system.incus.IncusCommandView;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class LxdContainerConsoleAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<LxdContainerStore>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<LxdContainerStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<LxdContainerStore> getApplicableClass() {
                return LxdContainerStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<LxdContainerStore> store) {
                return AppI18n.observable("serialConsole");
            }

            @Override
            public LabelGraphic getIcon(DataStoreEntryRef<LxdContainerStore> store) {
                return new LabelGraphic.IconGraphic("mdi2c-console");
            }

            @Override
            public boolean requiresValidStore() {
                return false;
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry store;

        @Override
        public void execute() throws Exception {
            var d = (LxdContainerStore) store.getStore();
            var view = new IncusCommandView(
                    d.getCmd().getStore().getHost().getStore().getOrStartSession());
            TerminalLauncher.open(store.getName(), view.console(d.getContainerName()));
        }
    }
}
