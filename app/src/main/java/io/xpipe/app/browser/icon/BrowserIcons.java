package io.xpipe.app.browser.icon;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.ext.FileEntry;

public class BrowserIcons {

    public static Comp<?> createDefaultFileIcon() {
        return PrettyImageHelper.ofFixedSizeSquare("browser/default_file.svg", 24);
    }

    public static Comp<?> createDefaultDirectoryIcon() {
        return PrettyImageHelper.ofFixedSizeSquare("browser/default_folder.svg", 24);
    }

    public static Comp<?> createContextMenuIcon(BrowserIconFileType type) {
        return PrettyImageHelper.ofFixedSizeSquare(type.getIcon(), 16);
    }

    public static Comp<?> createIcon(FileEntry entry) {
        return PrettyImageHelper.ofFixedSizeSquare(BrowserIconManager.getFileIcon(entry), 24);
    }
}
