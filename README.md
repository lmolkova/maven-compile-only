# A demo of truly optional dependency with Maven

This is a small proof of concept that shows how to create fully optional dependency in a library with Maven.
Such dependencies are available at compile time, but

1. they don't become transitive dependencies of projects that depend on this library
2. they don't show up on Maven central or in dependency graph when running `mvn dependency:tree`.
3. nothing fails if they are not available at runtime

Check out [Maven docs on optional dependencies](https://maven.apache.org/guides/introduction/introduction-to-optional-and-excludes-dependencies.html) and
[Gradle compile-only dependencies](https://blog.gradle.org/introducing-compile-only-dependencies).

You can easily achieve #1 by setting `optional` to `true` on the dependency.
The #2 needs extra POM file that doesn't list optional dependencies.
Gradle `compileOnly` dependencies give the combination of #1 and #2.

The #3 needs a special approach to writing code that ensures all usages of optional dependency are contained and guarded with runtime checks so that when
someone calls this feature, it's handled gracefully.

## Why optional dependency?

When a library introduces a runtime dependency, it imposes its version choice on everyone who uses it. Diamond dependency problems are challenging in general, but Maven's dependency resolution is particularly notorious for selecting seemingly random versions from the available options.

Some features are not required in runtime, such as logging, tracing, or metrics. We depend on user to put logging provider and OpenTelemetry on the classpath and
if they don't, there is no point for the library to do any logging or tracing - there is nothing to consume telemetry anyway.

Long story short, having a truly optional dependency seems great.

We can also proudly say that we don't have any dependencies. As a library author, when you see a package without any dependencies, you appreciate it a lot.
That's the reason behind #2 (don't show dependencies on Maven central or in dependency graph when running `mvn dependency:tree`).

## The approach here

The [library](./library/pom.xml) project uses good old optional dependency on `slf4j-api`

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.16</version>
    <optional>true</optional>
</dependency>
```

It works with Java 9 modules (see [module-info.java](./library/src/main/java/module-info.java)) using

```java
requires static org.slf4j;
```

We also maintain (or generate) another [pom-public.xml](./library/pom-public.xml) file that we'll use when publishing the artifact - it doesn't list dependency on `slf4j-api`.

We have a wrapper around slf4j `Logger`, naive implementation looks like this:

```java
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
```

Yes, we need to mirror all the APIs we're going to use.

### Running the demo

1. Build and deploy the library

```bash
cd /path/to/maven-compile-only/library
mvn clean verify
mvn deploy:deploy-file -Dfile=/path/to/maven-compile-only/library/target/library-1.0.0-beta.1.jar -DpomFile=/path/to/maven-compile-only/library/pom-public.xml -Durl=file://path/to/m2/cache
```

2. Run the app

```bash
cd /path/to/maven-compile-only/app
mvn clean verify
java -jar .\target\app-1.0-SNAPSHOT.jar
```

It should print

```log
SLF4J not available, falling back to console logging
console - Doing something
```

Now uncomment dependency on slf4j-simple in the [app/pom.xml](./app/pom.xml), rebuild and rerun the app:

```bash
mvn clean verify
java -jar .\target\app-1.0-SNAPSHOT.jar
```

It'd print

```log
[main] INFO io.maven_compile_only.library.TestClass - Doing something
```

We have absolutely optional dependency.

### Alternatives to this approach

1. Switch to Gradle. It means that we only need to write the code in a certain way and never rely on the dependency being available. Easy.
2. Use reflection. You can check if something is on the classpath at runtime and use it only if it's found. It can be almost as efficient as direct invocations, but it's
   hard to write and maintain, especially if API surface is relatively big.
3. You can do some maven-compile-plugin magic and add dependency on the classpath at compile time without ever declaring it in the POM like shown in this
   [excellent SO thread](https://stackoverflow.com/questions/3410548/maven-add-a-folder-or-jar-file-into-current-classpath). That does not play nice with IDEs and you have to keep hacking.

### Open questions

Still looking for an easy solution to would convert "internal" pom to the "public" one. If you know one, hit me up.
