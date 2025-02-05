package dev.tsok.globalid;

public interface IParser<T> {
    /**
     * Converts a string into an object of type T.
     */
    T parse(String value);

    /**
     * Converts an object of type T into a string.
     */
    String format(T value);
}
