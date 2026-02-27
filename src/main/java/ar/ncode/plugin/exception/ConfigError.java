package ar.ncode.plugin.exception;

public class ConfigError extends RuntimeException {

    public ConfigError(String message) {
        super(message);
    }

    public ConfigError(String message, Throwable cause) {
        super(message, cause);
    }

}
