package io.xpipe.app.comp.store;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.TooltipHelper;
import io.xpipe.app.core.AppI18n;

import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.input.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

public class StoreActiveComp extends SimpleComp {

    private final StoreEntryWrapper wrapper;

    public StoreActiveComp(StoreEntryWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    protected Region createSimple() {
        var c = new Circle(6);
        c.getStyleClass().add("dot");
        c.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                wrapper.stopSession();
                event.consume();
            }
        });
        var pane = new StackPane(c);
        pane.setAlignment(Pos.CENTER);
        pane.visibleProperty().bind(wrapper.getSessionActive());
        pane.getStyleClass().add("store-active-comp");
        Tooltip.install(pane, TooltipHelper.create(AppI18n.observable("sessionActive"), null));
        return pane;
    }
}
