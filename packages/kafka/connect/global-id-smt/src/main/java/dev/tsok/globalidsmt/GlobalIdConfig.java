package dev.tsok.globalidsmt;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;

public class GlobalIdConfig extends AbstractConfig {

    public static final String FIELD_NAME_CONFIG = "field.name";
    private static final String FIELD_NAME_DOC = "Name of the input field to transform.";

    public static final String FIELD_OUT_CONFIG = "field.out";
    private static final String FIELD_OUT_DOC = "Name of the output field to store the GlobalID string.";

    public static final String GLOBALID_TYPE_CONFIG = "globalid.type";
    private static final String GLOBALID_TYPE_DOC = "The type string used in GlobalId (e.g. 'Organization').";

    public static final String GLOBALID_PREFIX_CONFIG = "globalid.prefix";
    private static final String GLOBALID_PREFIX_DOC = "The prefix registered in the TypeRegistry (e.g. 'org').";

    public static final String GLOBALID_VERSION_CONFIG = "globalid.version";
    private static final String GLOBALID_VERSION_DOC = "Which parser version to use (e.g. '1.0.0').";

    public static final String PARSER_CLASS_CONFIG = "parser.class";
    private static final String PARSER_CLASS_DOC = "Fully-qualified name of the IParser implementation to parse/format the data.";

    public static ConfigDef CONFIG_DEF = new ConfigDef()
            .define(FIELD_NAME_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, FIELD_NAME_DOC)
            .define(FIELD_OUT_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.MEDIUM, FIELD_OUT_DOC)
            .define(GLOBALID_TYPE_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, GLOBALID_TYPE_DOC)
            .define(GLOBALID_PREFIX_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, GLOBALID_PREFIX_DOC)
            .define(GLOBALID_VERSION_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, GLOBALID_VERSION_DOC)
            .define(PARSER_CLASS_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, PARSER_CLASS_DOC);

    public GlobalIdConfig(Map<String, ?> originals) {
        super(CONFIG_DEF, originals);
    }

    public String fieldName() {
        return getString(FIELD_NAME_CONFIG);
    }

    public String fieldOut() {
        return getString(FIELD_OUT_CONFIG);
    }

    public String globalIdType() {
        return getString(GLOBALID_TYPE_CONFIG);
    }

    public String globalIdPrefix() {
        return getString(GLOBALID_PREFIX_CONFIG);
    }

    public String globalIdVersion() {
        return getString(GLOBALID_VERSION_CONFIG);
    }

    public String parserClass() {
        return getString(PARSER_CLASS_CONFIG);
    }
}
