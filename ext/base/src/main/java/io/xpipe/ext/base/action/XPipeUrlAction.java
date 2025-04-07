package io.xpipe.ext.base.action;

import io.xpipe.app.comp.store.StoreCreationDialog;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.JacksonMapper;

import lombok.Value;

import java.util.List;
import java.util.UUID;

public class XPipeUrlAction implements ActionProvider {

    @Override
    public LauncherCallSite getLauncherCallSite() {
        return new XPipeLauncherCallSite() {

            @Override
            public String getId() {
                return "xpipe";
            }

            @Override
            public Action createAction(List<String> args) throws Exception {
                switch (args.get(0)) {
                    case "addStore" -> {
                        var storeString = InPlaceSecretValue.builder()
                                .encryptedValue(args.get(1))
                                .build();
                        var store = JacksonMapper.parse(storeString.getSecretValue(), DataStore.class);
                        return new AddStoreAction(store);
                    }
                    case "launch" -> {
                        var entry = DataStorage.get()
                                .getStoreEntryIfPresent(UUID.fromString(args.get(1)))
                                .orElseThrow();
                        if (!entry.getValidity().isUsable()) {
                            return null;
                        }
                        var p = entry.getProvider();
                        return p.launchAction(entry);
                    }
                    case "action" -> {
                        var id = args.get(1);
                        ActionProvider provider = ActionProvider.ALL.stream()
                                .filter(actionProvider -> {
                                    return actionProvider.getLeafDataStoreCallSite() != null
                                            && id.equals(actionProvider.getId());
                                })
                                .findFirst()
                                .orElseThrow();
                        var entry = DataStorage.get()
                                .getStoreEntryIfPresent(UUID.fromString(args.get(2)))
                                .orElseThrow();

                        TrackEvent.withDebug("Parsed action")
                                .tag("id", id)
                                .tag("provider", provider.getId())
                                .tag("entry", entry.getUuid())
                                .handle();

                        if (!entry.getValidity().isUsable()) {
                            return null;
                        }
                        return new CallAction(provider, entry);
                    }
                    default -> {
                        return null;
                    }
                }
            }
        };
    }

    @Value
    static class CallAction implements ActionProvider.Action {

        ActionProvider actionProvider;
        DataStoreEntry entry;

        @Override
        public void execute() throws Exception {
            actionProvider.getLeafDataStoreCallSite().createAction(entry.ref()).execute();
        }
    }

    @Value
    static class AddStoreAction implements ActionProvider.Action {

        DataStore store;

        @Override
        public void execute() {
            if (store == null) {
                return;
            }

            var entry = DataStoreEntry.createNew(
                    UUID.randomUUID(),
                    StoreViewState.get()
                            .getActiveCategory()
                            .getValue()
                            .getCategory()
                            .getUuid(),
                    "",
                    store);
            StoreCreationDialog.showEdit(entry);
        }
    }
}
