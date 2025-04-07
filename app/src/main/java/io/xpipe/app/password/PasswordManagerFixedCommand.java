package io.xpipe.app.password;

import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.core.process.ShellScript;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("passwordManagerFixedCommand")
public abstract class PasswordManagerFixedCommand implements PasswordManager {

    protected abstract ShellScript getScript();

    @Override
    public synchronized String retrievePassword(String key) {
        var cmd = ExternalApplicationHelper.replaceVariableArgument(getScript().getValue(), "KEY", key);
        return PasswordManagerCommand.retrieveWithCommand(cmd);
    }
}
