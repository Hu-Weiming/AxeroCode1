package com.axercode.core.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ToolCallTest {

    @Test
    void createAssignsIdAndDefaultsBlankArgumentsJson() {
        ToolCall call = ToolCall.create("read_file", " ");

        assertNotNull(call.id());
        assertEquals("read_file", call.name());
        assertEquals("{}", call.argumentsJson());
    }

    @Test
    void createRejectsBlankToolName() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ToolCall.create(" ", "{}")
        );

        assertEquals("name must not be blank", exception.getMessage());
    }
}
