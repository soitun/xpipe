package io.xpipe.app.hub.action.impl;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.action.LeafStoreActionProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.comp.StoreCreationDialog;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.store.DataStore;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class EditStoreActionProvider implements LeafStoreActionProvider<DataStore> {

            @Override
            public AbstractAction createAction(DataStoreEntryRef<DataStore> ref) {
                return Action.builder().ref(ref).build();
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
                return AppI18n.observable("base.edit");
            }

            @Override
            public LabelGraphic getIcon(DataStoreEntryRef<DataStore> store) {
                return new LabelGraphic.IconGraphic("mdal-edit");
            }

            @Override
            public boolean requiresValidStore() {
                return false;
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<DataStore> o) {
                return true;
            }



        @Override
    public String getId() {
        return "editStore";
    }
@Jacksonized
@SuperBuilder
    static class Action extends StoreAction<DataStore> {

        @Override
        public void executeImpl() {
            StoreCreationDialog.showEdit(ref.get());
        }
    }
}
