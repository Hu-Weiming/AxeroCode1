# Step 6 - Spring Boot 3.x CLI Bootstrap

## Step Goal

Turn `axercode-cli` into the first executable Spring Boot 3.x console application and connect it to the Ollama provider stack for one-shot prompt execution.

## What Was Done

1. Added Spring Boot 3.x and Picocli dependencies to the CLI module.
2. Added the first executable CLI bootstrap application.
3. Added typed provider configuration properties and a Spring-managed `LlmProvider` bean.
4. Added `CliChatService` to build provider requests from console prompts.
5. Added `AxerCodeCliCommand` to parse `--prompt` and optional `--model` arguments.
6. Added a Picocli runner that executes the Spring-managed command inside the Boot application lifecycle.
7. Verified the generated executable jar against the real local Ollama runtime.

## Files Added or Changed

### Build and Config

- `pom.xml`
- `axercode-cli/pom.xml`
- `axercode-cli/src/main/resources/application.yml`

### Production Code

- `axercode-cli/src/main/java/com/axercode/cli/bootstrap/AxerCodeCliApplication.java`
- `axercode-cli/src/main/java/com/axercode/cli/bootstrap/PicocliRunner.java`
- `axercode-cli/src/main/java/com/axercode/cli/config/AxerCodeProviderProperties.java`
- `axercode-cli/src/main/java/com/axercode/cli/config/ProviderConfiguration.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/CliChatService.java`
- `axercode-cli/src/main/java/com/axercode/cli/command/AxerCodeCliCommand.java`

### Tests

- `axercode-cli/src/test/java/com/axercode/cli/service/CliChatServiceTest.java`
- `axercode-cli/src/test/java/com/axercode/cli/command/AxerCodeCliCommandTest.java`

## How It Was Implemented

## 1. Spring Boot 3.x Console Shell

`AxerCodeCliApplication` is the first real application entrypoint in the repository. It starts Spring Boot with `WebApplicationType.NONE`, so the app behaves like a console process rather than a server.

That keeps Step 6 aligned with the current goal: executable local CLI first, full REPL later.

## 2. Picocli + Spring Wiring

`AxerCodeCliCommand` is the root Picocli command. It accepts:

- `--prompt`
- optional `--model`

`PicocliRunner` is the bridge that executes the Spring-managed Picocli command when the Boot app starts. This is the first time the repository uses the `picocli-spring-boot-starter` integration path in practice.

## 3. Chat Service Layer

`CliChatService` is the small application service between the command layer and the provider layer.

Its responsibilities are:

- validate prompt input
- choose the model from CLI override or default properties
- build a `ProviderRequest`
- call the current `LlmProvider`
- convert a `ProviderResponse` into printable text

If the model asks for tool calls, the service currently returns a clear placeholder message instead of pretending tool execution exists already.

## 4. Provider Configuration

`AxerCodeProviderProperties` and `ProviderConfiguration` make the CLI runtime configurable while staying simple:

- default model: `qwen2.5:7b`
- default Ollama base URL: `http://127.0.0.1:11434`

The `LlmProvider` bean is currently backed by `OllamaChatProvider`.

## 5. Integration Bugs Found During Real Verification

This step uncovered two real integration problems that unit tests alone did not fully expose:

### Ollama metadata parsing

The real Ollama response includes fields like `created_at` and token timing metadata. The first adapter version failed because Jackson did not ignore unknown fields.

The fix was to make `OllamaChatResponse` ignore unknown properties.

### Maven Surefire on Windows + JDK 21

When running broader module tests, Surefire hit a classpath issue related to manifest-only jars and absolute Windows paths. The parent build was updated so Surefire:

- disables the URL classpath root check
- avoids manifest-only jars

That stabilized full-reactor test execution on this machine.

## TDD Evidence

This step used test-first development for the CLI behavior:

1. `CliChatServiceTest` failed first because the service and provider property classes did not exist.
2. `AxerCodeCliCommandTest` failed first because the Picocli command did not exist.
3. Minimal production code was added to satisfy those tests.
4. Additional failing provider tests were added when real Ollama behavior exposed missing metadata handling.
5. The provider adapter was corrected with the smallest possible fix.

## Verification Commands

The following commands were executed successfully in `D:\AeroCode1`:

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=CliChatServiceTest,AxerCodeCliCommandTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-provider-ollama -am test "-Dtest=OllamaChatProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am package
```

The generated jar was also executed successfully:

```bat
java -jar D:\AeroCode1\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --prompt=hello
```

Observed output:

```text
Hello! How can I assist you today? Feel free to ask me any questions or let me know if you need help with anything.
```

## Why This Step Matters

This is the first point where AxerCode is not just a collection of modules and tests. It is now an executable program that:

- starts as a Spring Boot 3.x console app
- accepts CLI input
- builds a provider request
- calls the local Ollama-backed provider
- prints a real model response

## Next Step

Step 7 should move toward richer interaction rather than single-shot execution:

- add a session-aware command shell
- start introducing multi-turn in-memory context
- prepare the path toward the full REPL step
