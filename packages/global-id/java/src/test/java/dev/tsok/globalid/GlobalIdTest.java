package dev.tsok.globalid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalIdTest {
    private TypeRegistry typeRegistry;
    private ParserRegistry parserRegistry;
    private Encoder encoder;
    private Decoder decoder;

    @BeforeEach
    void setUp() {
        typeRegistry = new TypeRegistry();
        parserRegistry = new ParserRegistry(typeRegistry);
        encoder = new Encoder(parserRegistry, typeRegistry);
        decoder = new Decoder(parserRegistry, typeRegistry);

        // Register type
        typeRegistry.registerType("Organization", "org");

        // Register a parser for version 1.0.0 for prefix "org"
        parserRegistry.registerParser("org", "1.0.0", new OrganizationLegacyIdentifierParserV1());
    }

    @Test
    void testEncodeDecode() {
        // Create a sample ID
        GlobalId<OrganizationData> globalId =
            new GlobalId<>("Organization", "1.0.0", new OrganizationData("uuid", "123"));

        // Encode
        String encoded = encoder.encode(globalId);
        assertTrue(encoded.startsWith("org_"));

        // Decode
        GlobalId<OrganizationData> decoded = decoder.decode(encoded);
        assertEquals("Organization", decoded.getType());
        assertEquals("1.0.0", decoded.getVersion());

        // Check value
        OrganizationData data = decoded.getValue();
        assertEquals("uuid", data.getId());
        assertEquals("123", data.getSystemId());
    }

    // A sample data class
    static class OrganizationData {
        private String id;
        private String systemId;

        public OrganizationData() {
            // default constructor for Jackson
        }

        public OrganizationData(String id, String systemId) {
            this.id = id;
            this.systemId = systemId;
        }

        public String getId() {
            return id;
        }
        public String getSystemId() {
            return systemId;
        }
    }

    // A sample parser that just JSON-serializes the object
    static class OrganizationLegacyIdentifierParserV1 implements IParser<OrganizationData> {
        @Override
        public OrganizationData parse(String value) {
            // In real code, use a JSON library:
            // Here we do a trivial approach for demonstration
            // e.g. {"id":"uuid","systemId":"123"}
            if (value == null || !value.contains("id")) {
                throw new IllegalArgumentException("Invalid JSON string for OrganizationData");
            }
            // A naive parse (for demonstration ONLY):
            String[] parts = value.replaceAll("[\"{}]", "").split(",");
            // parts[0] => id:uuid
            // parts[1] => systemId:123
            String[] idPair = parts[0].split(":");
            String[] systemIdPair = parts[1].split(":");

            return new OrganizationData(idPair[1], systemIdPair[1]);
        }

        @Override
        public String format(OrganizationData value) {
            // Return a naive JSON string
            return String.format("{\"id\":\"%s\",\"systemId\":\"%s\"}",
                value.getId(),
                value.getSystemId());
        }
    }
}
