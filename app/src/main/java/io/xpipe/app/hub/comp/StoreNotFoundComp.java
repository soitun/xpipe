package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleComp;

import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class StoreNotFoundComp extends SimpleComp {

    @Override
    public Region createSimple() {
        return new StackPane();
    }
}
