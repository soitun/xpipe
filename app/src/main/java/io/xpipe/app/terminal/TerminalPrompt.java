package io.xpipe.app.terminal;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellScript;
import io.xpipe.core.process.ShellTerminalInitCommand;
import io.xpipe.core.process.TerminalInitScriptConfig;
import io.xpipe.core.util.ValidationException;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface TerminalPrompt {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(TmuxTerminalMultiplexer.class);
        l.add(ZellijTerminalMultiplexer.class);
        l.add(ScreenTerminalMultiplexer.class);
        return l;
    }

    default void checkComplete() throws ValidationException {}

    String getDocsLink();

    void checkSupported(ShellControl sc) throws Exception;

    ShellTerminalInitCommand setup(ShellControl shellControl) throws Exception;
}
