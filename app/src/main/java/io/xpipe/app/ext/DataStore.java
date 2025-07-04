package io.xpipe.app.ext;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface DataStore {

    default boolean isComplete() {
        try {
            checkComplete();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    default void checkComplete() throws Throwable {}

    /**
     * Casts this instance to the required type without checking whether a cast is possible.
     */
    @SuppressWarnings("unchecked")
    default <DS extends DataStore> DS asNeeded() {
        return (DS) this;
    }
}
