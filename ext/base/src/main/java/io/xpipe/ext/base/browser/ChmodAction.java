package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserBranchAction;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TextField;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class ChmodAction implements BrowserBranchAction {

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2w-wrench");
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("chmod");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return model.getFileSystem().getShell().orElseThrow().getOsType() != OsType.WINDOWS;
    }

    @Override
    public List<BrowserLeafAction> getBranchingActions(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var custom = new Custom();
        return List.of(
                new Chmod("400"),
                new Chmod("600"),
                new Chmod("644"),
                new Chmod("700"),
                new Chmod("755"),
                new Chmod("777"),
                new Chmod("u+x"),
                new Chmod("a+x"),
                custom);
    }

    private static class Chmod implements BrowserLeafAction {

        private final String option;

        private Chmod(String option) {
            this.option = option;
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return new SimpleStringProperty(option);
        }

        @Override
        public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) throws Exception {
            model.getFileSystem()
                    .getShell()
                    .orElseThrow()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("chmod", option)
                            .addFiles(entries.stream()
                                    .map(browserEntry -> browserEntry
                                            .getRawFileEntry()
                                            .getPath()
                                            .toString())
                                    .toList()));
        }
    }

    private static class Custom implements BrowserLeafAction {
        @Override
        public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            var permissions = new SimpleStringProperty();
            var modal = ModalOverlay.of(
                    "chmodPermissions",
                    Comp.of(() -> {
                                var creationName = new TextField();
                                creationName.textProperty().bindBidirectional(permissions);
                                return creationName;
                            })
                            .prefWidth(350));
            modal.withDefaultButtons(() -> {
                if (permissions.getValue() == null) {
                    return;
                }

                model.runCommandAsync(
                        CommandBuilder.of()
                                .add("chmod", permissions.getValue())
                                .addFiles(entries.stream()
                                        .map(browserEntry -> browserEntry
                                                .getRawFileEntry()
                                                .getPath()
                                                .toString())
                                        .toList()),
                        false);
            });
            modal.show();
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return new SimpleStringProperty("...");
        }
    }
}
