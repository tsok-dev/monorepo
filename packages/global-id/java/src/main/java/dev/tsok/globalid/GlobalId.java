package dev.tsok.globalid;

public class GlobalId<T> {
    private final String type;
    private final String version;
    private final T value;

    public GlobalId(String type, String version, T value) {
        this.type = type;
        this.version = version;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public T getValue() {
        return value;
    }
}
