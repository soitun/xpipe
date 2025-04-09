package io.xpipe.app.issue;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.Hyperlinks;

public interface ErrorAction {

    static ErrorAction openDocumentation(String link) {
        return new ErrorAction() {
            @Override
            public String getName() {
                return AppI18n.get("openDocumentation");
            }

            @Override
            public String getDescription() {
                return AppI18n.get("openDocumentationDescription");
            }

            @Override
            public boolean handle(ErrorEvent event) {
                Hyperlinks.open(link);
                return false;
            }
        };
    }

    static ErrorAction ignore() {
        return new ErrorAction() {
            @Override
            public String getName() {
                return AppI18n.get("ignoreError");
            }

            @Override
            public String getDescription() {
                return AppI18n.get("ignoreErrorDescription");
            }

            @Override
            public boolean handle(ErrorEvent event) {
                if (!event.isReportable()) {
                    return true;
                }

                SentryErrorHandler.getInstance().handle(event);
                return true;
            }
        };
    }

    String getName();

    String getDescription();

    boolean handle(ErrorEvent event);
}
