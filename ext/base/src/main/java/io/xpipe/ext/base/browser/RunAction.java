package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FileEntry;
import io.xpipe.core.store.FileKind;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.stream.Stream;

public class RunAction extends MultiExecuteAction {

    private boolean isExecutable(FileEntry e) {
        if (e.getKind() != FileKind.FILE) {
            return false;
        }

        if (e.getInfo() != null && e.getInfo().possiblyExecutable()) {
            return true;
        }

        var shell = e.getFileSystem().getShell();
        if (shell.isEmpty()) {
            return false;
        }

        var os = shell.get().getOsType();
        if (os.equals(OsType.WINDOWS)
                && Stream.of("exe", "bat", "ps1", "cmd")
                        .anyMatch(s -> e.getPath().toString().endsWith(s))) {
            return true;
        }

        if (ShellDialects.isPowershell(shell.get())
                && Stream.of("ps1").anyMatch(s -> e.getPath().toString().endsWith(s))) {
            return true;
        }

        if (Stream.of("sh", "command").anyMatch(s -> e.getPath().toString().endsWith(s))) {
            return true;
        }

        return false;
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2p-play");
    }

    @Override
    public Category getCategory() {
        return Category.CUSTOM;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("run");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return entries.stream().allMatch(entry -> isExecutable(entry.getRawFileEntry()));
    }

    protected CommandBuilder createCommand(ShellControl sc, BrowserFileSystemTabModel model, BrowserEntry entry) {
        return CommandBuilder.of()
                .add(sc.getShellDialect()
                        .runScriptCommand(sc, entry.getRawFileEntry().getPath().toString()));
    }
}
