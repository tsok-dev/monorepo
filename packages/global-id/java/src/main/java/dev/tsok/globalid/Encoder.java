package dev.tsok.globalid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class Encoder {
    private static final ObjectMapper CBOR_MAPPER = new ObjectMapper(new CBORFactory());

    private final ParserRegistry parserRegistry;
    private final TypeRegistry typeRegistry;

    public Encoder(ParserRegistry parserRegistry, TypeRegistry typeRegistry) {
        this.parserRegistry = parserRegistry;
        this.typeRegistry = typeRegistry;
    }

    public <T> String encode(GlobalId<T> id) {
        // Validate
        if (id.getValue() == null) {
            throw new IllegalArgumentException("Value is not defined (null).");
        }
        // Get prefix for the type
        String prefix = typeRegistry.getPrefix(id.getType());
        if (prefix == null) {
            throw new IllegalArgumentException("Type '" + id.getType() + "' is not registered.");
        }

        // Get parser
        IParser<T> parser = parserRegistry.getParser(prefix, id.getVersion());
        // Convert the value to a string
        String formattedValue = parser.format(id.getValue());

        // CBOR encode [ formattedValue, version ]
        List<String> payload = Arrays.asList(formattedValue, id.getVersion());
        byte[] cborBytes;
        try {
            cborBytes = CBOR_MAPPER.writeValueAsBytes(payload);
        } catch (Exception e) {
            throw new RuntimeException("Error while CBOR-encoding payload", e);
        }

        // Convert to Base64 (URL-safe style)
        String base64 = toUrlSafeBase64(cborBytes);

        return prefix + "_" + base64;
    }

    private String toUrlSafeBase64(byte[] data) {
        String base64 = Base64.getEncoder().encodeToString(data);
        // Replace + and / with URL-safe chars - and _
        // Remove trailing '='
        return base64
            .replace('+', '-')
            .replace('/', '_')
            .replaceAll("=+$", "");
    }
}
