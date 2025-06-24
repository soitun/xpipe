package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

public class CategoryRemoveExchange extends BeaconInterface<CategoryRemoveExchange.Request> {

    @Override
    public String getPath() {
        return "/category/remove";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        List<UUID> categories;

        boolean removeChildrenCategories;

        boolean removeContents;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
