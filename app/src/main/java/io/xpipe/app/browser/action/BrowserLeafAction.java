package io.xpipe.app.browser.action;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.comp.base.TooltipHelper;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.ThreadHelper;

import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public interface BrowserLeafAction extends BrowserAction {

    void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) throws Exception;

    default Button toButton(Region root, BrowserFileSystemTabModel model, List<BrowserEntry> selected) {
        var b = new Button();
        b.setOnAction(event -> {
            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(model.getBusy(), () -> {
                    if (model.getFileSystem() == null) {
                        return;
                    }

                    // Start shell in case we exited
                    model.getFileSystem().getShell().orElseThrow().start();
                    execute(model, selected);
                });
            });
            event.consume();
        });
        var name = getName(model, selected);
        Tooltip.install(b, TooltipHelper.create(name, getShortcut()));
        var graphic = getIcon(model, selected);
        if (graphic != null) {
            b.setGraphic(graphic);
        }
        b.setMnemonicParsing(false);
        b.accessibleTextProperty().bind(name);
        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (getShortcut() != null && getShortcut().match(event)) {
                b.fire();
                event.consume();
            }
        });

        b.setDisable(!isActive(model, selected));
        model.getCurrentPath().addListener((observable, oldValue, newValue) -> {
            b.setDisable(!isActive(model, selected));
        });

        if (getProFeatureId() != null
                && !LicenseProvider.get().getFeature(getProFeatureId()).isSupported()) {
            b.setDisable(true);
            b.setGraphic(new FontIcon("mdi2p-professional-hexagon"));
        }

        return b;
    }

    default MenuItem toMenuItem(BrowserFileSystemTabModel model, List<BrowserEntry> selected) {
        var name = getName(model, selected);
        var mi = new MenuItem();
        mi.textProperty().bind(BindingsHelper.map(name, s -> {
            if (getProFeatureId() != null) {
                return LicenseProvider.get().getFeature(getProFeatureId()).suffix(s);
            }
            return s;
        }));
        mi.setOnAction(event -> {
            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(model.getBusy(), () -> {
                    if (model.getFileSystem() == null) {
                        return;
                    }

                    // Start shell in case we exited
                    model.getFileSystem().getShell().orElseThrow().start();
                    execute(model, selected);
                });
            });
            event.consume();
        });
        if (getShortcut() != null) {
            mi.setAccelerator(getShortcut());
        }
        var graphic = getIcon(model, selected);
        if (graphic != null) {
            mi.setGraphic(graphic);
        }
        mi.setMnemonicParsing(false);
        mi.setDisable(!isActive(model, selected));

        if (getProFeatureId() != null
                && !LicenseProvider.get().getFeature(getProFeatureId()).isSupported()) {
            mi.setDisable(true);
        }

        return mi;
    }

    default String getId() {
        return null;
    }
}
