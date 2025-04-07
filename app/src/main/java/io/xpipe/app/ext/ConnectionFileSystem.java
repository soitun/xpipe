package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FileEntry;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.store.FileSystem;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Getter
public class ConnectionFileSystem implements FileSystem {

    @JsonIgnore
    protected final ShellControl shellControl;

    public ConnectionFileSystem(ShellControl shellControl) {
        this.shellControl = shellControl;
    }

    @Override
    public long getFileSize(FilePath file) throws Exception {
        return Long.parseLong(shellControl
                .getShellDialect()
                .queryFileSize(shellControl, file.toString())
                .readStdoutOrThrow());
    }

    @Override
    public Optional<ShellControl> getShell() {
        return Optional.of(shellControl);
    }

    @Override
    public FileSystem open() throws Exception {
        shellControl.start();

        var d = shellControl.getShellDialect().getDumbMode();
        if (!d.supportsAnyPossibleInteraction()) {
            shellControl.close();
            try {
                d.throwIfUnsupported();
            } catch (Exception e) {
                throw ErrorEvent.expected(e);
            }
        }

        if (!shellControl.getTtyState().isPreservesOutput()
                || !shellControl.getTtyState().isSupportsInput()) {
            var ex = new UnsupportedOperationException(
                    "Shell has a PTY allocated and as a result does not support file system operations.");
            ErrorEvent.preconfigure(ErrorEvent.fromThrowable(ex).documentationLink(DocumentationLink.TTY));
            throw ex;
        }

        shellControl.checkLicenseOrThrow();

        return this;
    }

    @Override
    public InputStream openInput(FilePath file) throws Exception {
        return shellControl
                .getShellDialect()
                .getFileReadCommand(shellControl, file.toString())
                .startExternalStdout();
    }

    @Override
    public OutputStream openOutput(FilePath file, long totalBytes) throws Exception {
        var cmd =
                shellControl.getShellDialect().createStreamFileWriteCommand(shellControl, file.toString(), totalBytes);
        cmd.setExitTimeout(Duration.ofMillis(Long.MAX_VALUE));
        return cmd.startExternalStdin();
    }

    @Override
    public boolean fileExists(FilePath file) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .createFileExistsCommand(shellControl, file.toString())
                .start()) {
            return pc.discardAndCheckExit();
        }
    }

    @Override
    public void delete(FilePath file) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .deleteFileOrDirectory(shellControl, file.toString())
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void copy(FilePath file, FilePath newFile) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .getFileCopyCommand(shellControl, file.toString(), newFile.toString())
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void move(FilePath file, FilePath newFile) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .getFileMoveCommand(shellControl, file.toString(), newFile.toString())
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void mkdirs(FilePath file) throws Exception {
        try (var pc = shellControl
                .command(
                        CommandBuilder.ofFunction(proc -> proc.getShellDialect().getMkdirsCommand(file.toString())))
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void touch(FilePath file) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .getFileTouchCommand(shellControl, file.toString())
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void symbolicLink(FilePath linkFile, FilePath targetFile) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .symbolicLink(shellControl, linkFile.toString(), targetFile.toString())
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public boolean directoryExists(FilePath file) throws Exception {
        return shellControl
                .getShellDialect()
                .directoryExists(shellControl, file.toString())
                .executeAndCheck();
    }

    @Override
    public void directoryAccessible(FilePath file) throws Exception {
        var current = shellControl.executeSimpleStringCommand(
                shellControl.getShellDialect().getPrintWorkingDirectoryCommand());
        shellControl.command(shellControl.getShellDialect().getCdCommand(file.toString()));
        shellControl.command(shellControl.getShellDialect().getCdCommand(current));
    }

    @Override
    public Stream<FileEntry> listFiles(FilePath file) throws Exception {
        return shellControl.getShellDialect().listFiles(this, shellControl, file.toString());
    }

    @Override
    public List<FilePath> listRoots() throws Exception {
        return shellControl
                .getShellDialect()
                .listRoots(shellControl)
                .map(s -> FilePath.of(s))
                .toList();
    }

    @Override
    public void close() {
        // In case the shell control is already in an invalid state, this operation might fail
        // Since we are only closing, just swallow all exceptions
        try {
            shellControl.close();
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).omit().expected().handle();
        }
    }
}
