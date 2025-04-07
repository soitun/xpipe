package io.xpipe.app.beacon.impl;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BlobManager;
import io.xpipe.app.ext.ConnectionFileSystem;
import io.xpipe.beacon.api.FsWriteExchange;

import com.sun.net.httpserver.HttpExchange;
import lombok.SneakyThrows;

public class FsWriteExchangeImpl extends FsWriteExchange {

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var shell = AppBeaconServer.get().getCache().getShellSession(msg.getConnection());
        var fs = new ConnectionFileSystem(shell.getControl());
        try (var in = BlobManager.get().getBlob(msg.getBlob());
                var os = fs.openOutput(msg.getPath(), in.available())) {
            in.transferTo(os);
        }
        return Response.builder().build();
    }
}
