package com.axercode.cli.bootstrap;

import com.axercode.cli.command.AxerCodeCliCommand;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Native-image reflection hints for Picocli command metadata discovery.
 */
public class CliNativeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(
                AxerCodeCliCommand.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.INTROSPECT_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS
        );
    }
}
