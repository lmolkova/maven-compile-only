package io.maven_compile_only.library;

public class TestClass {
    private static final OptionalLogger LOGGER = OptionalLogger.getLogger(TestClass.class);
    public void doSomething() {
        LOGGER.logInfo("Doing something");
    }
}
