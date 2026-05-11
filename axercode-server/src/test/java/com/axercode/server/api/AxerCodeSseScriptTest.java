package com.axercode.server.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class AxerCodeSseScriptTest {

    @Test
    void sseParserPreservesLeadingSpacesInTokenData() throws Exception {
        String nodeExecutable = resolveNodeExecutable()
                .orElseThrow(() -> new IllegalStateException("Node.js executable could not be resolved."));
        Path testScript = Path.of("src", "test", "js", "sse.test.js").toAbsolutePath();

        Process process = new ProcessBuilder(nodeExecutable, "--test", testScript.toString())
                .directory(Path.of("").toAbsolutePath().toFile())
                .redirectErrorStream(true)
                .start();

        boolean finished = process.waitFor(20, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }

        String output = new String(process.getInputStream().readAllBytes());
        assertTrue(finished, "Node-based SSE parser test timed out.");
        assertEquals(0, process.exitValue(), output);
    }

    private Optional<String> resolveNodeExecutable() {
        for (String candidate : new String[] {
                System.getenv("NODE_EXECUTABLE"),
                "D:\\nodejss\\node.exe",
                "node"
        }) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            if (looksExecutable(candidate)) {
                return Optional.of(candidate);
            }
        }
        Assumptions.abort("Node.js executable is not available for frontend parser verification.");
        return Optional.empty();
    }

    private boolean looksExecutable(String candidate) {
        try {
            Process process = new ProcessBuilder(candidate, "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
