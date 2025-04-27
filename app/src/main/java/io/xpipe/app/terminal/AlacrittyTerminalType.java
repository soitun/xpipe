package io.xpipe.app.terminal;

import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;

public interface AlacrittyTerminalType extends ExternalTerminalType, TrackableTerminalType {

    ExternalTerminalType ALACRITTY_WINDOWS = new Windows();
    ExternalTerminalType ALACRITTY_LINUX = new Linux();
    ExternalTerminalType ALACRITTY_MAC_OS = new MacOs();

    @Override
    default String getWebsite() {
        return "https://github.com/alacritty/alacritty";
    }

    @Override
    default TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW;
    }

    @Override
    default boolean isRecommended() {
        return false;
    }

    @Override
    default boolean useColoredTitle() {
        return false;
    }

    class Windows extends SimplePathType implements AlacrittyTerminalType {

        public Windows() {
            super("app.alacritty", "alacritty", true);
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            var b = CommandBuilder.of();

            //            if (configuration.getColor() != null) {
            //                b.add("-o")
            //                        .addQuoted("colors.primary.background='%s'"
            //                                .formatted(configuration.getColor().toHexString()));
            //            }

            // Alacritty is bugged and will not accept arguments with spaces even if they are correctly passed/escaped
            // So this will not work when the script file has spaces
            return b.add("-t")
                    .addQuoted(configuration.getCleanTitle())
                    .add("-e")
                    .add(configuration.getDialectLaunchCommand());
        }
    }

    class Linux extends SimplePathType implements AlacrittyTerminalType {

        public Linux() {
            super("app.alacritty", "alacritty", true);
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-t")
                    .addQuoted(configuration.getCleanTitle())
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    }

    class MacOs extends MacOsType implements AlacrittyTerminalType {

        public MacOs() {
            super("app.alacritty", "Alacritty");
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("Alacritty.app")
                            .add("-n", "--args", "-t")
                            .addQuoted(configuration.getCleanTitle())
                            .add("-e")
                            .addFile(configuration.getScriptFile()));
        }
    }
}
