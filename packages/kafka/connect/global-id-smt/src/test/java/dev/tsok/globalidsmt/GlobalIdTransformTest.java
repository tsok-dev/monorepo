package dev.tsok.globalidsmt;

import dev.tsok.globalid.GlobalId;
import dev.tsok.globalid.StringParser;
import org.apache.kafka.connect.data.*;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalIdTransformTest {

    private GlobalIdTransform<SourceRecord> transform;

    @BeforeEach
    void setup() {
        transform = new GlobalIdTransform<>();
    }

    @AfterEach
    void teardown() {
        transform.close();
    }

    @Test
    void testSchemalessTransform() {
        // 1. Configure the transform
        Map<String, String> props = new HashMap<>();
        props.put(GlobalIdConfig.FIELD_NAME_CONFIG, "raw_field");
        props.put(GlobalIdConfig.FIELD_OUT_CONFIG, "global_id");
        props.put(GlobalIdConfig.GLOBALID_TYPE_CONFIG, "Organization");
        props.put(GlobalIdConfig.GLOBALID_PREFIX_CONFIG, "org");
        props.put(GlobalIdConfig.GLOBALID_VERSION_CONFIG, "1.0.0");
        props.put(GlobalIdConfig.PARSER_CLASS_CONFIG, StringParser.class.getName());

        transform.configure(props);

        // 2. Create a schemaless record (map value)
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("raw_field", "some-string-value");
        SourceRecord record = new SourceRecord(
                null, null, 
                "test-topic", 
                null, // no key schema
                null, // no key 
                null, // no value schema
                valueMap
        );

        // 3. Apply transformation
        SourceRecord transformedRecord = transform.apply(record);

        // 4. Validate
        @SuppressWarnings("unchecked")
        Map<String, Object> transformedValueMap = (Map<String, Object>) transformedRecord.value();
        assertTrue(transformedValueMap.containsKey("global_id"), "Output field must be present");
        
        // The transformed value should be a GlobalId-encoded string, e.g. "org_xxx..."
        String globalIdString = (String) transformedValueMap.get("global_id");
        assertNotNull(globalIdString);
        assertTrue(globalIdString.startsWith("org_"));
    }

    @Test
    void testStructTransform() {
        // 1. Configure transform
        Map<String, String> props = new HashMap<>();
        props.put(GlobalIdConfig.FIELD_NAME_CONFIG, "raw_field");
        props.put(GlobalIdConfig.FIELD_OUT_CONFIG, "global_id");
        props.put(GlobalIdConfig.GLOBALID_TYPE_CONFIG, "Organization");
        props.put(GlobalIdConfig.GLOBALID_PREFIX_CONFIG, "org");
        props.put(GlobalIdConfig.GLOBALID_VERSION_CONFIG, "1.0.0");
        props.put(GlobalIdConfig.PARSER_CLASS_CONFIG, StringParser.class.getName());

        transform.configure(props);

        // 2. Create a schema
        Schema schema = SchemaBuilder.struct()
                .name("TestSchema")
                .field("raw_field", Schema.STRING_SCHEMA)
                // For demonstration, let's assume the output field is also in the schema
                .field("global_id", Schema.OPTIONAL_STRING_SCHEMA)
                .build();

        // 3. Create a struct with data
        Struct struct = new Struct(schema)
                .put("raw_field", "some-other-value");

        // 4. Create a SourceRecord
        SourceRecord record = new SourceRecord(
                null, null,
                "test-topic",
                null,
                null,
                null,
                // value schema
                schema,
                // value
                struct
        );

        // 5. Apply transformation
        SourceRecord transformedRecord = transform.apply(record);

        // 6. Validate
        assertNotNull(transformedRecord.valueSchema());
        assertEquals(schema, transformedRecord.valueSchema()); // Using the same schema

        Struct updatedStruct = (Struct) transformedRecord.value();
        String globalIdString = updatedStruct.getString("global_id");
        assertNotNull(globalIdString);
        assertTrue(globalIdString.startsWith("org_"));
    }

    @Test
    void testMissingField() {
        // If the input field is missing, the transform should do nothing (or your desired behavior)
        Map<String, String> props = new HashMap<>();
        props.put(GlobalIdConfig.FIELD_NAME_CONFIG, "non_existent");
        props.put(GlobalIdConfig.FIELD_OUT_CONFIG, "global_id");
        props.put(GlobalIdConfig.GLOBALID_TYPE_CONFIG, "Organization");
        props.put(GlobalIdConfig.GLOBALID_PREFIX_CONFIG, "org");
        props.put(GlobalIdConfig.GLOBALID_VERSION_CONFIG, "1.0.0");
        props.put(GlobalIdConfig.PARSER_CLASS_CONFIG, StringParser.class.getName());

        transform.configure(props);

        // Schemaless example
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("raw_field", "this field won't be transformed");

        SourceRecord record = new SourceRecord(null, null, "test-topic", null, null, null, valueMap);

        SourceRecord transformedRecord = transform.apply(record);

        // The transform should not modify anything
        @SuppressWarnings("unchecked")
        Map<String, Object> transformedMap = (Map<String, Object>) transformedRecord.value();
        assertFalse(transformedMap.containsKey("global_id"));
        assertEquals("this field won't be transformed", transformedMap.get("raw_field"));
    }

    @Test
    void testNullValue() {
        // If the value is null, the transform should either pass it through or skip
        Map<String, String> props = new HashMap<>();
        props.put(GlobalIdConfig.FIELD_NAME_CONFIG, "raw_field");
        props.put(GlobalIdConfig.FIELD_OUT_CONFIG, "global_id");
        props.put(GlobalIdConfig.GLOBALID_TYPE_CONFIG, "Organization");
        props.put(GlobalIdConfig.GLOBALID_PREFIX_CONFIG, "org");
        props.put(GlobalIdConfig.GLOBALID_VERSION_CONFIG, "1.0.0");
        props.put(GlobalIdConfig.PARSER_CLASS_CONFIG, StringParser.class.getName());

        transform.configure(props);

        SourceRecord record = new SourceRecord(null, null, "test-topic", null, null, null, null);
        SourceRecord transformedRecord = transform.apply(record);

        // Likely no change
        assertNull(transformedRecord.value());
        assertNull(transformedRecord.valueSchema());
    }

    @Test
    void testInvalidParserClass() {
        // Provide a parser class that doesn't exist or doesn't implement IParser
        Map<String, String> props = new HashMap<>();
        props.put(GlobalIdConfig.FIELD_NAME_CONFIG, "raw_field");
        props.put(GlobalIdConfig.FIELD_OUT_CONFIG, "global_id");
        props.put(GlobalIdConfig.GLOBALID_TYPE_CONFIG, "Organization");
        props.put(GlobalIdConfig.GLOBALID_PREFIX_CONFIG, "org");
        props.put(GlobalIdConfig.GLOBALID_VERSION_CONFIG, "1.0.0");
        props.put(GlobalIdConfig.PARSER_CLASS_CONFIG, "java.lang.String"); // invalid, not an IParser

        // Expect an exception on configure
        assertThrows(RuntimeException.class, () -> transform.configure(props));
    }
}
