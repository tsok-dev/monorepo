package dev.tsok.globalid;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps a "type" to a short prefix and vice versa:
 * e.g. "Organization" -> "org"
 */
public class TypeRegistry {
    private final Map<String, String> typeToPrefix = new HashMap<>();
    private final Map<String, String> prefixToType = new HashMap<>();

    public void registerType(String type, String prefix) {
        if (typeToPrefix.containsKey(type)) {
            throw new IllegalArgumentException("Type is already registered: " + type);
        }
        if (prefixToType.containsKey(prefix)) {
            throw new IllegalArgumentException("Prefix is already registered: " + prefix);
        }
        typeToPrefix.put(type, prefix);
        prefixToType.put(prefix, type);
    }

    public String getPrefix(String type) {
        return typeToPrefix.get(type);
    }

    public String getType(String prefix) {
        return prefixToType.get(prefix);
    }
}
