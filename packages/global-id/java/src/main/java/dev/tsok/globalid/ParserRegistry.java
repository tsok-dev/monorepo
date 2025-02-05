package dev.tsok.globalid;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages versioned parsers for each prefix.
 * E.g., for prefix "org", we might have multiple versions like "1.0.0", "2.0.0".
 */
public class ParserRegistry {
    // Each prefix has a map of version -> parser
    private final Map<String, Map<String, IParser<?>>> registry = new HashMap<>();
    private final TypeRegistry typeRegistry;

    public ParserRegistry(TypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    public <T> void registerParser(String prefix, String version, IParser<T> parser) {
        // Ensure the prefix is known in TypeRegistry
        if (typeRegistry.getType(prefix) == null) {
            throw new IllegalArgumentException("Prefix '" + prefix + "' is not registered in TypeRegistry.");
        }

        registry.computeIfAbsent(prefix, k -> new HashMap<>()).put(version, parser);
    }

    @SuppressWarnings("unchecked")
    public <T> IParser<T> getParser(String prefix, String version) {
        Map<String, IParser<?>> versionMap = registry.get(prefix);
        if (versionMap == null) {
            throw new IllegalArgumentException("No parsers registered for prefix: " + prefix);
        }
        IParser<?> parser = versionMap.get(version);
        if (parser == null) {
            throw new IllegalArgumentException(
                "No parser registered for prefix: " + prefix + " and version: " + version
            );
        }
        return (IParser<T>) parser;
    }
}
