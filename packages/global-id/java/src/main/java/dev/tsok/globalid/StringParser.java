package dev.tsok.globalid;

public class StringParser implements IParser<String> {
    @Override
    public String parse(String value) {
        // No transformation needed for plain string
        return value;
    }

    @Override
    public String format(String value) {
        // No transformation needed for plain string
        return value;
    }
}
