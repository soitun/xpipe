package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.action.impl.NewDirectoryActionProvider;
import io.xpipe.app.browser.action.impl.NewFileActionProvider;
import io.xpipe.app.browser.action.impl.NewLinkActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.icon.BrowserIcons;
import io.xpipe.app.browser.menu.BrowserMenuBranchProvider;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FilePath;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class NewItemMenuProvider implements BrowserMenuBranchProvider {

    @Override
    public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new LabelGraphic.IconGraphic("mdi2p-plus-box-outline");
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.MUTATION;
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("new");
    }

    @Override
    public List<BrowserMenuLeafProvider> getBranchingActions(
            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return List.of(
                new BrowserMenuLeafProvider() {
                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var name = new SimpleStringProperty();
                        var modal = ModalOverlay.of(
                                "newFile",
                                Comp.of(() -> {
                                            var creationName = new TextField();
                                            creationName.textProperty().bindBidirectional(name);
                                            return creationName;
                                        })
                                        .prefWidth(350));
                        modal.withDefaultButtons(() -> {
                            if (name.getValue() == null || name.getValue().isEmpty()) {
                                return;
                            }

                            var builder = NewFileActionProvider.Action.builder();
                            builder.initEntries(model, entries);
                            builder.name(name.getValue());
                            builder.build().executeAsync();
                        });
                        modal.show();
                    }

                    @Override
                    public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return new LabelGraphic.CompGraphic(BrowserIcons.createDefaultFileIcon());
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("file");
                    }
                },
                new BrowserMenuLeafProvider() {
                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var name = new SimpleStringProperty();
                        var modal = ModalOverlay.of(
                                "newDirectory",
                                Comp.of(() -> {
                                            var creationName = new TextField();
                                            creationName.textProperty().bindBidirectional(name);
                                            return creationName;
                                        })
                                        .prefWidth(350));
                        modal.withDefaultButtons(() -> {
                            if (name.getValue() == null || name.getValue().isEmpty()) {
                                return;
                            }

                            var builder = NewDirectoryActionProvider.Action.builder();
                            builder.initEntries(model, entries);
                            builder.name(name.getValue());
                            builder.build().executeAsync();
                        });
                        modal.show();
                    }

                    @Override
                    public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return new LabelGraphic.CompGraphic(BrowserIcons.createDefaultDirectoryIcon());
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("directory");
                    }
                },
                new BrowserMenuLeafProvider() {
                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var linkName = new SimpleStringProperty();
                        var target = new SimpleStringProperty();
                        var modal = ModalOverlay.of(
                                "base.newLink",
                                new OptionsBuilder()
                                        .name("linkName")
                                        .addString(linkName)
                                        .name("targetPath")
                                        .addString(target)
                                        .buildComp()
                                        .prefWidth(350));
                        modal.withDefaultButtons(() -> {
                            if (linkName.getValue() == null
                                    || linkName.getValue().isEmpty()
                                    || target.getValue() == null
                                    || target.getValue().isEmpty()) {
                                return;
                            }

                            var builder = NewLinkActionProvider.Action.builder();
                            builder.initEntries(model, entries);
                            builder.name(linkName.getValue());
                            builder.target(FilePath.of(target.getValue()));
                            builder.build().executeAsync();
                        });
                        modal.show();
                    }

                    @Override
                    public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return new LabelGraphic.CompGraphic(BrowserIcons.createDefaultFileIcon());
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("symbolicLink");
                    }

                    @Override
                    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return model.getFileSystem().getShell().orElseThrow().getOsType() != OsType.WINDOWS;
                    }
                });
    }
}
