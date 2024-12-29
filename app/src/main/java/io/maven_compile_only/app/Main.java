package io.maven_compile_only.app;

import io.maven_compile_only.library.TestClass;

public class Main {
    public static void main(String[] args) {
        var testClass = new TestClass();
        testClass.doSomething();
    }
}