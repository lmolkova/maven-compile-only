package io.maven_compile_only.library;

import org.slf4j.Logger;

class OptionalLogger {
    private static final boolean INITIALIZED;

    static {
        boolean initialized = false;
        try {
            Class.forName("org.slf4j.Logger");
            initialized = true;
        } catch (ClassNotFoundException e) {
            System.out.println("SLF4J not available, falling back to console logging");
        }
        INITIALIZED = initialized;
    }

    private final Logger logger;
    public static OptionalLogger getLogger(Class<?> clazz) {
        return INITIALIZED ? new OptionalLogger(org.slf4j.LoggerFactory.getLogger(clazz)) : new OptionalLogger(null);
    }

    private OptionalLogger(Logger logger) {
        this.logger = logger;
    }

    public void logInfo(String message) {
        if (logger != null) {
            logger.info(message);
        } else {
            System.out.printf("console - %s\n", message);
        }
    }
}
