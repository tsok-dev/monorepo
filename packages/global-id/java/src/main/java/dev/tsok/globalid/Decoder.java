package dev.tsok.globalid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Base64;
import java.util.List;

public class Decoder {
    private static final ObjectMapper CBOR_MAPPER = new ObjectMapper(new CBORFactory());

    private final ParserRegistry parserRegistry;
    private final TypeRegistry typeRegistry;

    public Decoder(ParserRegistry parserRegistry, TypeRegistry typeRegistry) {
        this.parserRegistry = parserRegistry;
        this.typeRegistry = typeRegistry;
    }

    public <T> GlobalId<T> decode(String encodedId) {
        if (encodedId == null || encodedId.isEmpty()) {
            throw new IllegalArgumentException("Encoded ID cannot be null or empty.");
        }

        // Split on the first underscore
        int underscoreIndex = encodedId.indexOf('_');
        if (underscoreIndex < 1) {
            // We expect at least one character for prefix, then '_', then payload
            throw new IllegalArgumentException("Invalid encoded ID format (missing or misplaced underscore).");
        }

        String prefix = encodedId.substring(0, underscoreIndex);
        String base64Payload = encodedId.substring(underscoreIndex + 1);

        // Convert URL-safe base64 back to normal
        byte[] cborBytes = fromUrlSafeBase64(base64Payload);

        // CBOR decode => [ data, version ]
        List<Object> decodedList;
        try {
            decodedList = CBOR_MAPPER.readValue(cborBytes, new TypeReference<List<Object>>(){});
        } catch (Exception e) {
            throw new RuntimeException("Error while CBOR-decoding payload", e);
        }

        if (decodedList.size() != 2) {
            throw new IllegalArgumentException("Decoded payload should contain [data, version].");
        }

        String data = (String) decodedList.get(0);
        String version = (String) decodedList.get(1);

        // Look up the parser
        IParser<T> parser = parserRegistry.getParser(prefix, version);
        // Parse the data
        T parsedValue = parser.parse(data);

        // Convert prefix back to type
        String type = typeRegistry.getType(prefix);
        if (type == null) {
            throw new IllegalArgumentException(
                "Prefix '" + prefix + "' is not registered in the TypeRegistry."
            );
        }

        return new GlobalId<>(type, version, parsedValue);
    }

    private byte[] fromUrlSafeBase64(String base64) {
        // Restore + and /
        String padded = base64.replace('-', '+').replace('_', '/');
        // Pad with '=' up to multiple of 4
        while (padded.length() % 4 != 0) {
            padded += "=";
        }
        return Base64.getDecoder().decode(padded);
    }
}
