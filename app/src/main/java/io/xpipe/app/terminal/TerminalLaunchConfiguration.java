package io.xpipe.app.terminal;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LicenseRequiredException;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import lombok.*;
import lombok.experimental.NonFinal;

import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Value
@RequiredArgsConstructor
@AllArgsConstructor
public class TerminalLaunchConfiguration {
    DataStoreColor color;
    String coloredTitle;
    String cleanTitle;
    boolean preferTabs;
    String scriptContent;
    ShellDialect scriptDialect;

    @NonFinal
    FilePath scriptFile = null;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());

    public static TerminalLaunchConfiguration create(
            UUID request,
            DataStoreEntry entry,
            String cleanTitle,
            String adjustedTitle,
            boolean preferTabs,
            boolean alwaysPromptRestart)
            throws Exception {
        var color = entry != null ? DataStorage.get().getEffectiveColor(entry) : null;

        if (!AppPrefs.get().enableTerminalLogging().get()) {
            var d = ProcessControlProvider.get().getEffectiveLocalDialect();
            var launcherScript = d.terminalLauncherScript(request, adjustedTitle, alwaysPromptRestart);
            var config = new TerminalLaunchConfiguration(
                    entry != null ? color : null, adjustedTitle, cleanTitle, preferTabs, launcherScript, d);
            return config;
        }

        var feature = LicenseProvider.get().getFeature("logging");
        var supported = feature.isSupported();
        if (!supported) {
            throw new LicenseRequiredException(feature);
        }

        var logDir = AppProperties.get().getDataDir().resolve("sessions");
        Files.createDirectories(logDir);
        var logName = OsFileSystem.ofLocal()
                .makeFileSystemCompatible(FilePath.of(DataStorage.get().getStoreEntryDisplayName(entry) + " ("
                        + DATE_FORMATTER.format(Instant.now()) + ").log"))
                .toString()
                .replaceAll(" ", "_");
        var logFile = logDir.resolve(logName);
        try (var sc = LocalShell.getShell().start()) {
            if (OsType.getLocal() == OsType.WINDOWS) {
                var launcherScript = ScriptHelper.createExecScript(
                        ShellDialects.POWERSHELL,
                        sc,
                        ShellDialects.POWERSHELL.terminalLauncherScript(request, adjustedTitle, alwaysPromptRestart));
                var content =
                        """
                              echo 'Transcript started, output file is "sessions\\%s"'
                              Start-Transcript -Force -LiteralPath "%s" > $Out-Null
                              & %s
                              Stop-Transcript > $Out-Null
                              echo 'Transcript stopped, output file is "sessions\\%s"'
                              """
                                .formatted(
                                        logFile.getFileName().toString(),
                                        logFile,
                                        launcherScript,
                                        logFile.getFileName().toString());
                var config = new TerminalLaunchConfiguration(
                        entry != null ? color : null,
                        adjustedTitle,
                        cleanTitle,
                        preferTabs,
                        content,
                        ShellDialects.POWERSHELL);
                return config;
            } else {
                var found = sc.command(sc.getShellDialect().getWhichCommand("script"))
                        .executeAndCheck();
                if (!found) {
                    var suffix = sc.getOsType() == OsType.MACOS
                            ? "This command is available in the util-linux package which can be installed via homebrew."
                            : "This command is available in the util-linux package.";
                    throw ErrorEventFactory.expected(new IllegalStateException(
                            "Logging requires the script command to be installed. " + suffix));
                }

                var launcherScript = ScriptHelper.createExecScript(
                        sc, sc.getShellDialect().terminalLauncherScript(request, adjustedTitle, alwaysPromptRestart));
                var content = sc.getOsType() == OsType.MACOS || sc.getOsType() == OsType.BSD
                        ? """
                       echo "Transcript started, output file is sessions/%s"
                       script -e -q "%s" "%s"
                       echo "Transcript stopped, output file is sessions/%s"
                       """
                                .formatted(logFile.getFileName(), logFile, launcherScript, logFile.getFileName())
                        : """
                       echo "Transcript started, output file is sessions/%s"
                       script --quiet --command "%s" "%s"
                       echo "Transcript stopped, output file is sessions/%s"
                       """
                                .formatted(logFile.getFileName(), launcherScript, logFile, logFile.getFileName());
                var config = new TerminalLaunchConfiguration(
                        entry != null ? color : null,
                        adjustedTitle,
                        cleanTitle,
                        preferTabs,
                        content,
                        sc.getShellDialect());
                return config;
            }
        }
    }

    public TerminalLaunchConfiguration withScript(ShellDialect d, String content) {
        return new TerminalLaunchConfiguration(color, coloredTitle, cleanTitle, preferTabs, content, d);
    }

    @SneakyThrows
    public synchronized FilePath getScriptFile() {
        if (scriptFile == null) {
            scriptFile = ScriptHelper.createExecScript(scriptDialect, LocalShell.getShell(), scriptContent);
        }
        return scriptFile;
    }

    public synchronized CommandBuilder getDialectLaunchCommand() {
        var open = scriptDialect.getOpenScriptCommand(getScriptFile().toString());
        return open;
    }
}
