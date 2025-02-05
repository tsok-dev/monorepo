package dev.tsok.globalidsmt;

import dev.tsok.globalid.*;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.*;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.common.config.ConfigDef;
import java.util.HashMap;
import java.util.Map;

public class GlobalIdTransform<R extends ConnectRecord<R>> implements Transformation<R> {

    private GlobalIdConfig config;
    private TypeRegistry typeRegistry;
    private ParserRegistry parserRegistry;
    private Encoder encoder; // to convert to a GlobalId string

    @Override
    public void configure(Map<String, ?> configs) {
        this.config = new GlobalIdConfig(configs);

        // Create registries
        this.typeRegistry = new TypeRegistry();
        // Register the prefix => type mapping
        typeRegistry.registerType(config.globalIdType(), config.globalIdPrefix());

        this.parserRegistry = new ParserRegistry(typeRegistry);

        // Create an instance of the parser
        IParser<?> parser = instantiateParser(config.parserClass());
        // Register parser
        parserRegistry.registerParser(config.globalIdPrefix(), config.globalIdVersion(), parser);

        // Create an encoder
        this.encoder = new Encoder(parserRegistry, typeRegistry);
    }

    @Override
    public R apply(R record) {
        Object value = record.value();
        Schema schema = record.valueSchema();

        if (value == null) {
            // skip null values or pass them unchanged
            return record;
        }

        // We'll handle both schemaless (Map) and schema-based (Struct) here
        if (schema == null) {
            // Schemaless
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> valueMap = (Map<String, Object>) value;
                transformSchemaless(valueMap);
            }
            return record;
        } else if (schema.type() == Schema.Type.STRUCT) {
            // With schema
            if (!(value instanceof Struct)) {
                throw new DataException("Expected Struct but found " + value.getClass());
            }
            Struct struct = (Struct) value;

            // Build a new struct with possibly the same schema or a modified one
            // If you want to keep the same schema, be sure the output field is in that schema
            // or build a new schema with the additional field.
            Struct updatedStruct = transformStruct(struct);

            return record.newRecord(
                    record.topic(),
                    record.kafkaPartition(),
                    record.keySchema(),
                    record.key(),
                    schema, // keep the same schema or a new one if you added a new field
                    updatedStruct,
                    record.timestamp()
            );
        } else {
            // Not a Struct, skip or throw an exception as needed
            return record;
        }
    }

    private void transformSchemaless(Map<String, Object> valueMap) {
        String fieldName = config.fieldName();
        if (!valueMap.containsKey(fieldName)) {
            // Field not present, skip
            return;
        }
        Object rawValue = valueMap.get(fieldName);
        if (rawValue instanceof String) {
            String globalIdString = createGlobalIdString((String) rawValue);
            // Put it in the output field
            valueMap.put(config.fieldOut(), globalIdString);
        }
    }

    private Struct transformStruct(Struct struct) {
        String fieldName = config.fieldName();
        if (struct.schema().field(fieldName) == null) {
            // Field doesn't exist, skip
            return struct;
        }

        // Retrieve the existing field value
        Object rawValue = struct.get(fieldName);

        // We are creating a new struct instance so we can update or add fields
        // If the output field is not in the schema, you might need to create a new schema
        Struct updatedStruct = new Struct(struct.schema());
        // Copy old field values first
        for (Field f : struct.schema().fields()) {
            updatedStruct.put(f.name(), struct.get(f.name()));
        }

        // Now transform our target field
        if (rawValue instanceof String) {
            String globalIdString = createGlobalIdString((String) rawValue);
            updatedStruct.put(config.fieldOut(), globalIdString);
        }

        return updatedStruct;
    }

    private String createGlobalIdString(String rawValue) {
        // Build the GlobalId object
        GlobalId<String> globalId = new GlobalId<>(
                config.globalIdType(),
                config.globalIdVersion(),
                rawValue
        );
        // Encode it as a string
        return encoder.encode(globalId);
    }

    @Override
    public void close() {
        // Cleanup if necessary
    }

    @Override
    public ConfigDef config() {
        return GlobalIdConfig.CONFIG_DEF;
    }

    @SuppressWarnings("unchecked")
    private IParser<?> instantiateParser(String parserClassName) {
        try {
            Class<?> clazz = Class.forName(parserClassName);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (!(instance instanceof IParser)) {
                throw new IllegalArgumentException(
                    "Provided parser.class does not implement IParser: " + parserClassName
                );
            }
            return (IParser<?>) instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate parser class: " + parserClassName, e);
        }
    }
}
