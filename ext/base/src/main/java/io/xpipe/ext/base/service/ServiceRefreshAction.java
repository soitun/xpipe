package io.xpipe.ext.base.service;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;

import io.xpipe.app.util.LabelGraphic;
import javafx.beans.value.ObservableValue;

import lombok.Value;

public class ServiceRefreshAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<FixedServiceCreatorStore>() {

            @Override
            public boolean isMajor(DataStoreEntryRef<FixedServiceCreatorStore> o) {
                return true;
            }

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<FixedServiceCreatorStore> store) {
                return new Action(store);
            }

            @Override
            public Class<FixedServiceCreatorStore> getApplicableClass() {
                return FixedServiceCreatorStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<FixedServiceCreatorStore> store) {
                return AppI18n.observable("refreshServices");
            }

            @Override
            public LabelGraphic getIcon(DataStoreEntryRef<FixedServiceCreatorStore> store) {
                return new LabelGraphic.IconGraphic("mdi2w-web");
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<FixedServiceCreatorStore> o) {
                return o.getStore().allowManualServicesRefresh();
            }
        };
    }

    @Override
    public BatchDataStoreCallSite<?> getBatchDataStoreCallSite() {
        return new BatchDataStoreCallSite<FixedServiceCreatorStore>() {

            @Override
            public boolean isApplicable(DataStoreEntryRef<FixedServiceCreatorStore> o) {
                return o.getStore().allowManualServicesRefresh();
            }

            @Override
            public ObservableValue<String> getName() {
                return AppI18n.observable("refreshServices");
            }

            @Override
            public String getIcon() {
                return "mdi2w-web";
            }

            @Override
            public Class<?> getApplicableClass() {
                return FixedServiceCreatorStore.class;
            }

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<FixedServiceCreatorStore> store) {
                return new Action(store);
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntryRef<FixedServiceCreatorStore> ref;

        @Override
        public void execute() {
            ref.get().setExpanded(true);
            var e = DataStorage.get()
                    .addStoreIfNotPresent(
                            "Services",
                            FixedServiceGroupStore.builder().parent(ref).build());
            DataStorage.get().refreshChildren(e);
        }
    }
}
