package io.xpipe.app.action;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.UuidHelper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ActionUrls {

    private static String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static List<String> nodeToString(JsonNode node) {
        if (node.isTextual()) {
            return List.of(encodeValue(node.asText()));
        }

        if (node.isArray()) {
            var list = new ArrayList<String>();
            for (JsonNode c : node) {
                var r = nodeToString(c);
                if (r.size() == 1) {
                    list.add(r.getFirst());
                }
            }
            return list;
        }

        var enc = InPlaceSecretValue.of(node.toPrettyString()).getEncryptedValue();
        return List.of(enc);
    }

    @SneakyThrows
    public static String toUrl(AbstractAction action) {
        if (!(action instanceof SerializableAction sa)) {
            return null;
        }

        var json = sa.toNode();
        var parsed =
                JacksonMapper.getDefault().treeToValue(json, new TypeReference<LinkedHashMap<String, JsonNode>>() {});

        Map<String, List<String>> requestParams = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> e : parsed.entrySet()) {
            var value = nodeToString(e.getValue());
            requestParams.put(e.getKey(), value);
        }

        String encodedURL = requestParams.keySet().stream()
                .map(key -> {
                    var vals = requestParams.get(key);
                    return vals.stream().map(s -> key + "=" + s).collect(Collectors.joining("&"));
                })
                .collect(Collectors.joining("&", "xpipe://action?", ""));
        return encodedURL;
    }

    public static Optional<AbstractAction> parse(String queryString) throws Exception {
        var query = splitQuery(queryString);

        var id = query.get("id");
        if (id == null || id.size() != 1) {
            return Optional.empty();
        }

        var provider = ActionProvider.ALL.stream()
                .filter(actionProvider -> id.getFirst().equals(actionProvider.getId()))
                .findFirst();
        if (provider.isEmpty()) {
            return Optional.empty();
        }

        var clazz = provider.get().getActionClass();
        if (clazz.isEmpty()) {
            return Optional.empty();
        }

        if (!SerializableAction.class.isAssignableFrom(clazz.get())) {
            return Optional.empty();
        }

        var stores = query.get("ref");
        if (stores == null || stores.isEmpty()) {
            return Optional.empty();
        }

        for (String store : stores) {
            var uuid = UuidHelper.parse(store);
            if (uuid.isEmpty()) {
                throw new IllegalArgumentException("Invalid store id: " + store);
            }

            var entry = DataStorage.get().getStoreEntryIfPresent(uuid.get());
            if (entry.isEmpty()) {
                throw new IllegalArgumentException("Store not found for id: " + store);
            }

            if (!entry.get().getValidity().isUsable()) {
                throw new IllegalArgumentException(
                        "Store " + DataStorage.get().getStorePath(entry.get()) + " is incomplete");
            }
        }

        var fixedMap = query.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> entry.getValue().size() == 1 ? entry.getValue().getFirst() : entry.getValue()));
        var json = (ObjectNode) JacksonMapper.getDefault().valueToTree(fixedMap);
        var instance = ActionJacksonMapper.parse(json);
        return Optional.ofNullable(instance);
    }

    private static Map<String, List<String>> splitQuery(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyMap();
        }

        return Arrays.stream(query.split("&"))
                .map(ActionUrls::splitQueryParameter)
                .collect(Collectors.groupingBy(
                        AbstractMap.SimpleImmutableEntry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private static AbstractMap.SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new AbstractMap.SimpleImmutableEntry<>(
                URLDecoder.decode(key, StandardCharsets.UTF_8),
                value != null ? URLDecoder.decode(value, StandardCharsets.UTF_8) : null);
    }
}
