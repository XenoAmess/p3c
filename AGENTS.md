# P3C - Alibaba Java Coding Guidelines (XenoAmess TPM)

This is a Third-Party Maintenance (TPM) fork of the original [alibaba/p3c](https://github.com/alibaba/p3c) project, maintained by XenoAmess.

## Project Overview

P3C is a comprehensive Java coding guidelines implementation that provides:

1. **PMD Rule Implementations** (`p3c-pmd/`) - 54+ rules based on PMD for static code analysis
2. **IntelliJ IDEA Plugin** (`idea-plugin/`) - Real-time code inspection and analysis plugin
3. **Eclipse Plugin** (`eclipse-plugin/`) - Original Eclipse plugin (not actively maintained by TPM)
4. **Code Formatter Configs** (`p3c-formatter/`) - Eclipse code style templates
5. **GitBook Documentation** (`p3c-gitbook/`) - Chinese documentation for the coding guidelines

### Key Features of this TPM

- Always compatible with latest JetBrains IDEA releases/EAP versions
- Uses latest dependencies versions
- Blacklist/Whitelist configuration mechanism for rules/classes
- JSON configuration support (alternative to X8L format)
- Additional rules beyond original P3C rules

## Project Structure

```
p3c/
├── p3c-pmd/                    # PMD rule implementations (Maven project)
│   ├── src/main/java/          # Java rule implementations
│   ├── src/main/resources/     # Ruleset XML files and messages
│   ├── src/test/java/          # Unit tests for rules
│   └── pom.xml                 # Maven configuration
├── idea-plugin/                # IntelliJ IDEA plugin (Gradle project)
│   ├── p3c-common/             # Common plugin code (Kotlin)
│   ├── p3c-idea/               # Plugin packaging and resources
│   ├── build.gradle            # Root Gradle build script
│   └── settings.gradle         # Gradle project settings
├── eclipse-plugin/             # Eclipse plugin (not actively maintained)
├── p3c-formatter/              # Eclipse code formatter templates
├── p3c-gitbook/                # Documentation in Chinese
├── p3c_config.x8l              # Project configuration file (X8L format)
├── build.cmd                   # Windows build script
├── deploy.cmd                  # Windows deployment script
└── sonar.cmd                   # SonarQube analysis script
```

## Technology Stack

### p3c-pmd Module
- **Language**: Java 8
- **Build Tool**: Maven 3+
- **Core Dependencies**:
  - PMD 6.55.0 (pmd-java, pmd-vm)
  - Kotlin 2.3.10 (stdlib)
  - X8L 2.3.9 (configuration format)
  - Commons Lang3 3.20.0
  - Commons IO 2.21.0
  - Jackson 2.21.1
- **Testing**: JUnit 4.13.2
- **Code Quality**: Checkstyle 11.1.0, PMD Maven Plugin, JaCoCo

### idea-plugin Module
- **Language**: Kotlin (primary), Java
- **Build Tool**: Gradle with IntelliJ Platform Plugin
- **JDK Version**: Java 17 (for building)
- **Target IDEA Version**: 2024.1 (configurable via `gradle.properties`)
- **Core Dependencies**:
  - Freemarker 2.3.33
  - Javassist 3.30.2-GA
  - p3c-pmd (local dependency)

## Build Instructions

### Prerequisites
- JDK 8+ (for p3c-pmd)
- JDK 17+ (for idea-plugin Gradle build)
- Maven 3.x

### Building p3c-pmd

```bash
cd p3c-pmd
./mvnw clean install -Dmaven.javadoc.skip=false
```

For deployment (requires GPG key):
```bash
./mvnw clean deploy -Dmaven.javadoc.skip=false -Psonatype-oss-release
```

### Building IDEA Plugin

```bash
cd idea-plugin
./gradlew clean buildPlugin
```

To run plugin in development IDE:
```bash
./gradlew runIde
```

To run with specific IDEA version:
```bash
./gradlew runIde -Pidea_version=2024.1
```

### Full Build (Windows)

Use the provided batch script:
```batch
build.cmd
```

### CI/CD

The project uses GitHub Actions (`.github/workflows/build.yml`):
- Builds on Windows, Ubuntu, and macOS
- Uses Java 17
- Builds both p3c-pmd and idea-plugin
- Uploads plugin artifacts

## Code Organization

### PMD Rules Structure

Rules are organized by category in `p3c-pmd/src/main/resources/rulesets/java/`:

| Ruleset File | Category |
|-------------|----------|
| `ali-comment.xml` | Code comments and Javadoc |
| `ali-concurrent.xml` | Concurrency and threading |
| `ali-constant.xml` | Constants conventions |
| `ali-exception.xml` | Exception handling |
| `ali-flowcontrol.xml` | Flow control statements |
| `ali-naming.xml` | Naming conventions |
| `ali-oop.xml` | Object-oriented programming |
| `ali-orm.xml` | ORM-related rules |
| `ali-other.xml` | Other miscellaneous rules |
| `ali-set.xml` | Collection usage |
| `xenoamess-additional.xml` | Additional TPM-specific rules |
| `xenoamess-deprecated.xml` | Deprecated rules |

Each rule implementation is located in `p3c-pmd/src/main/java/com/xenoamess/p3c/pmd/lang/java/rule/` following the same category structure.

### Rule Base Classes

- `AbstractAliRule` - Base class for all Java rules with type resolution
- `AbstractAliXpathRule` - Base class for XPath-based rules
- `AbstractPojoRule` - Base class for POJO-related rules
- `AbstractAliCommentRule` - Base class for comment-related rules

### Internationalization

Message resources are in `p3c-pmd/src/main/resources/`:
- `messages.xml` - Chinese (default)
- `messages_en.xml` - English

## Configuration Mechanism

The project supports flexible configuration via `p3c_config.x8l` (or `p3c_config.json`) file.

### Configuration File Location
- For IDEA plugin: `$project_root/p3c_config.x8l`
- For Maven PMD: `$mvn_run_directory/p3c_config.x8l`

### Configuration Options

```xml
<com.alibaba.p3c.pmd.config version=0.0.1>
    <!-- Rule-specific configurations -->
    <rule_config>
        <LowerCamelCaseVariableNamingRule>
            <WHITE_LIST>DAOImpl&URL&URI>
        >
    >
    <!-- Global rule blacklist -->
    <rule_blacklist>
        PackageNamingRule&SomeOtherRule>
    >
    <!-- Global class blacklist -->
    <class_blacklist>
        Console>
    >
    <!-- Global package blacklist -->
    <package_blacklist>
        com.example.legacy>
    >
    <!-- Rule-class pair blacklist -->
    <rule_class_pair_blacklist>
        <SomeClass [>SomeRule&AnotherRule>
    >
>
```

### JSON Alternative

Configuration can also be done via JSON format (see `idea-plugin/README.md` for full example).

## Testing

### PMD Rule Tests

Tests are located in `p3c-pmd/src/test/java/` following the same package structure as main code.

Test resource files (XML format with test cases) are in:
`p3c-pmd/src/test/resources/com/xenoamess/p3c/pmd/lang/java/rule/<category>/xml/`

Run tests:
```bash
cd p3c-pmd
./mvnw test
```

### Test File Format

Test XML files contain test cases with code snippets and expected violations:
```xml
<test-code>
    <description>test description</description>
    <expected-problems>1</expected-problems>
    <code>
<![CDATA[
// test code here
]]>
    </code>
</test-code>
```

## Code Style Guidelines

### For Java Code

The project enforces code quality via Maven plugins:

1. **Checkstyle**: Configuration in `p3c-pmd/src/site/resources/checkstyle/checkstyle.xml`
2. **PMD**: Self-checking using its own rulesets during build
3. **Animal Sniffer**: Ensures Java 8 compatibility
4. **JaCoCo**: Code coverage reporting

Run with enforcement:
```bash
./mvnw clean install -Penforce
```

### For Kotlin Code

- Follow standard Kotlin conventions
- Use 4-space indentation
- UTF-8 encoding

## Version Information

- **Current Version**: 2.2.4.3x
- **Group ID**: `com.xenoamess.p3c`
- **Artifact ID**: `p3c-pmd`

## Maven Dependency

```xml
<dependency>
    <groupId>com.xenoamess.p3c</groupId>
    <artifactId>p3c-pmd</artifactId>
    <version>2.2.4.3x</version>
</dependency>
```

## Security Considerations

1. The project uses Log4j 2.25.3 (addresses Log4Shell vulnerability)
2. All dependencies are regularly updated via dependabot
3. GPG signing is required for releases

## Contributing

- PRs should target `idea-plugin` and `p3c-pmd` modules
- Eclipse plugin PRs are not accepted (maintainer has no expertise)
- Ensure all tests pass before submitting PR
- Follow existing code style and conventions

## License

Apache License 2.0 (see `license.txt`)

## Additional Resources

- [Alibaba Java Coding Guidelines (Chinese PDF)](Java开发手册（嵩山版）.pdf)
- [English Documentation](https://alibaba.github.io/Alibaba-Java-Coding-Guidelines)
- [JetBrains Plugin Page](https://plugins.jetbrains.com/plugin/14109-alibaba-java-coding-guidelines-xenoamess-tpm-)
