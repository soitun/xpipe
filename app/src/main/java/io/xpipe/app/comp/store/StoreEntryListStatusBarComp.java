package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.augment.ContextMenuAugment;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.*;
import io.xpipe.core.store.DataStore;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;

import java.util.ArrayList;
import java.util.List;

public class StoreEntryListStatusBarComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var checkbox = new StoreEntryBatchSelectComp(StoreViewState.get().getCurrentTopLevelSection());
        var l = new LabelComp(Bindings.createStringBinding(
                () -> {
                    return AppI18n.get(
                            "connectionsSelected",
                            StoreViewState.get()
                                    .getEffectiveBatchModeSelection()
                                    .getList()
                                    .size());
                },
                StoreViewState.get().getEffectiveBatchModeSelection().getList(),
                AppI18n.activeLanguage()));
        l.minWidth(Region.USE_PREF_SIZE);
        l.apply(struc -> {
            struc.get().setAlignment(Pos.CENTER);
        });
        var actions = new ToolbarComp(createActions());
        var close = new IconButtonComp("mdi2c-close", () -> {
            StoreViewState.get().getBatchMode().setValue(false);
        });
        close.apply(struc -> {
            struc.get().getStyleClass().remove(Styles.FLAT);
            struc.get().minWidthProperty().bind(struc.get().heightProperty());
            struc.get().prefWidthProperty().bind(struc.get().heightProperty());
            struc.get().maxWidthProperty().bind(struc.get().heightProperty());
        });
        var bar = new HorizontalComp(List.of(
                checkbox, Comp.hspacer(12), l, Comp.hspacer(20), actions, Comp.hspacer(), Comp.hspacer(20), close));
        bar.apply(struc -> {
            struc.get().setFillHeight(true);
            struc.get().setAlignment(Pos.CENTER_LEFT);
        });
        bar.minHeight(40);
        bar.prefHeight(40);
        bar.styleClass("bar");
        bar.styleClass("store-entry-list-status-bar");
        return bar.createRegion();
    }

    private ObservableList<Comp<?>> createActions() {
        var l = new DerivedObservableList<ActionProvider>(FXCollections.observableArrayList(), true);
        StoreViewState.get().getEffectiveBatchModeSelection().getList().addListener((ListChangeListener<
                        ? super StoreEntryWrapper>)
                c -> {
                    l.setContent(getCompatibleActionProviders());
                });
        return l.<Comp<?>>mapped(actionProvider -> {
                    return buildButton(actionProvider);
                })
                .getList();
    }

    private List<ActionProvider> getCompatibleActionProviders() {
        var l = StoreViewState.get().getEffectiveBatchModeSelection().getList();
        if (l.isEmpty()) {
            return List.of();
        }

        var all = new ArrayList<>(ActionProvider.ALL);
        for (StoreEntryWrapper w : l) {
            var actions = ActionProvider.ALL.stream()
                    .filter(actionProvider -> {
                        var s = actionProvider.getBatchDataStoreCallSite();
                        if (s == null) {
                            return false;
                        }

                        if (!s.getApplicableClass()
                                .isAssignableFrom(w.getStore().getValue().getClass())) {
                            return false;
                        }

                        if (!s.isApplicable(w.getEntry().ref())) {
                            return false;
                        }

                        return true;
                    })
                    .toList();
            all.removeIf(actionProvider -> !actions.contains(actionProvider));
        }
        return all;
    }

    @SuppressWarnings("unchecked")
    private <T extends DataStore> Comp<?> buildButton(ActionProvider p) {
        ActionProvider.BatchDataStoreCallSite<T> s =
                (ActionProvider.BatchDataStoreCallSite<T>) p.getBatchDataStoreCallSite();
        if (s == null) {
            return Comp.empty();
        }

        List<DataStoreEntryRef<T>> childrenRefs =
                StoreViewState.get().getEffectiveBatchModeSelection().getList().stream()
                        .map(storeEntryWrapper -> storeEntryWrapper.getEntry().<T>ref())
                        .toList();
        var batchActions = s.getChildren(childrenRefs);
        var button = new ButtonComp(
                s.getName(), new SimpleObjectProperty<>(new LabelGraphic.IconGraphic(s.getIcon())), () -> {
                    if (batchActions.size() > 0) {
                        return;
                    }

                    runActions(s);
                });
        if (batchActions.size() > 0) {
            button.apply(new ContextMenuAugment<>(
                    mouseEvent -> mouseEvent.getButton() == MouseButton.PRIMARY, keyEvent -> false, () -> {
                        var cm = ContextMenuHelper.create();
                        s.getChildren(childrenRefs).forEach(childProvider -> {
                            var menu = buildMenuItemForAction(childrenRefs, childProvider);
                            cm.getItems().add(menu);
                        });
                        return cm;
                    }));
        }
        return button;
    }

    @SuppressWarnings("unchecked")
    private <T extends DataStore> MenuItem buildMenuItemForAction(List<DataStoreEntryRef<T>> batch, ActionProvider a) {
        ActionProvider.BatchDataStoreCallSite<T> s =
                (ActionProvider.BatchDataStoreCallSite<T>) a.getBatchDataStoreCallSite();
        var name = s.getName();
        var icon = s.getIcon();
        var children = s.getChildren(batch);
        if (children.size() > 0) {
            var menu = new Menu();
            menu.textProperty().bind(name);
            menu.setGraphic(new LabelGraphic.IconGraphic(icon).createGraphicNode());
            var items = children.stream()
                    .filter(actionProvider -> actionProvider.getBatchDataStoreCallSite() != null)
                    .map(c -> buildMenuItemForAction(batch, c))
                    .toList();
            menu.getItems().addAll(items);
            return menu;
        } else {
            var item = new MenuItem();
            item.textProperty().bind(name);
            item.setGraphic(new LabelGraphic.IconGraphic(icon).createGraphicNode());
            item.setOnAction(event -> {
                runActions(s);
                event.consume();
                if (event.getTarget() instanceof Menu m) {
                    m.getParentPopup().hide();
                }
            });
            return item;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends DataStore> void runActions(ActionProvider.BatchDataStoreCallSite<?> s) {
        ThreadHelper.runFailableAsync(() -> {
            var l = new ArrayList<>(
                    StoreViewState.get().getEffectiveBatchModeSelection().getList());
            var mapped = l.stream().map(w -> w.getEntry().<T>ref()).toList();
            var action = ((ActionProvider.BatchDataStoreCallSite<T>) s).createAction(mapped);
            if (action != null) {
                action.execute();
            }
        });
    }
}
