package io.xpipe.ext.base.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;

import io.xpipe.app.util.LabelGraphic;
import javafx.beans.value.ObservableValue;

import lombok.Value;

public class StoreStopAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<StoppableStore>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<StoppableStore> store) {
                return new Action(store);
            }

            @Override
            public Class<StoppableStore> getApplicableClass() {
                return StoppableStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<StoppableStore> store) {
                return AppI18n.observable("stop");
            }

            @Override
            public LabelGraphic getIcon(DataStoreEntryRef<StoppableStore> store) {
                return new LabelGraphic.IconGraphic("mdi2s-stop");
            }

            @Override
            public boolean requiresValidStore() {
                return false;
            }
        };
    }

    @Override
    public BatchDataStoreCallSite<?> getBatchDataStoreCallSite() {
        return new BatchDataStoreCallSite<StoppableStore>() {

            @Override
            public ObservableValue<String> getName() {
                return AppI18n.observable("stop");
            }

            @Override
            public String getIcon() {
                return "mdi2s-stop";
            }

            @Override
            public Class<?> getApplicableClass() {
                return StoppableStore.class;
            }

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<StoppableStore> store) {
                return new Action(store);
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntryRef<StoppableStore> entry;

        @Override
        public void execute() throws Exception {
            entry.getStore().stop();
        }
    }
}
