package com.axercode.cli.service;

import com.axercode.cli.config.AxerCodeShellProperties;
import com.axercode.cli.shell.ShellCommandResult;
import com.axercode.cli.shell.SlashCommandDispatcher;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Interactive CLI shell with a JLine terminal path for production and a Reader/Writer path for deterministic tests.
 */
@Service
public class InteractiveShellService {

    private final CliChatService cliChatService;
    private final SessionStore sessionStore;
    private final ShellStateStore shellStateStore;
    private final AxerCodeShellProperties shellProperties;
    private final ToolFeedbackFormatter toolFeedbackFormatter = new ToolFeedbackFormatter();
    private final SlashCommandDispatcher slashCommandDispatcher = new SlashCommandDispatcher();

    InteractiveShellService(CliChatService cliChatService, SessionStore sessionStore) {
        this(cliChatService, sessionStore, new InMemoryShellStateStore(), new AxerCodeShellProperties());
    }

    @Autowired
    public InteractiveShellService(
            CliChatService cliChatService,
            SessionStore sessionStore,
            ShellStateStore shellStateStore,
            AxerCodeShellProperties shellProperties
    ) {
        this.cliChatService = cliChatService;
        this.sessionStore = sessionStore;
        this.shellStateStore = shellStateStore;
        this.shellProperties = shellProperties;
    }

    public InteractiveShellService(
            CliChatService cliChatService,
            SessionStore sessionStore,
            AxerCodeShellProperties shellProperties
    ) {
        this(cliChatService, sessionStore, new InMemoryShellStateStore(), shellProperties);
    }

    public int runInteractive(String modelOverride) {
        try (Terminal terminal = buildSystemTerminal()) {
            return run(terminal, modelOverride);
        } catch (IOException exception) {
            System.err.println("[AxerCode] Interactive shell failed: " + exception.getMessage());
            return 1;
        }
    }

    public int run(Reader reader, PrintWriter writer, String modelOverride) {
        writer.println("AxerCode interactive shell started. Type /help for commands.");

        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            while (true) {
                writer.print("axer> ");
                writer.flush();

                String line = bufferedReader.readLine();
                if (line == null) {
                    writer.println();
                    writer.println("[AxerCode] End of input. Exiting interactive shell.");
                    return 0;
                }

                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if (!handleLine(trimmed, writer, modelOverride)) {
                    return 0;
                }
            }
        } catch (IOException exception) {
            writer.println("[AxerCode] Interactive shell failed: " + exception.getMessage());
            return 1;
        }
    }

    int run(Terminal terminal, String modelOverride) {
        PrintWriter writer = terminal.writer();
        writer.println("AxerCode interactive shell started. Type /help for commands.");
        writer.flush();

        LineReader lineReader = createLineReader(terminal);
        try {
            while (true) {
                String line;
                try {
                    line = lineReader.readLine("axer> ");
                } catch (UserInterruptException exception) {
                    continue;
                } catch (EndOfFileException exception) {
                    writer.println("[AxerCode] End of input. Exiting interactive shell.");
                    writer.flush();
                    saveHistory(lineReader);
                    return 0;
                }

                if (line == null) {
                    writer.println("[AxerCode] End of input. Exiting interactive shell.");
                    writer.flush();
                    saveHistory(lineReader);
                    return 0;
                }

                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if (!handleLine(trimmed, writer, modelOverride)) {
                    saveHistory(lineReader);
                    return 0;
                }
            }
        } finally {
            writer.flush();
        }
    }

    boolean handleLine(String trimmed, PrintWriter writer, String modelOverride) {
        if (trimmed.startsWith("/")) {
            ShellCommandResult result = slashCommandDispatcher.handle(
                    trimmed,
                    sessionStore,
                    shellStateStore,
                    cliChatService.resolveModel(modelOverride)
            );
            for (String outputLine : result.outputLines()) {
                writer.println(outputLine);
            }
            writer.flush();
            return result.continueShell();
        }

        StringBuilder streamedReply = new StringBuilder();
        CliChatTurn turn = cliChatService.continueConversationStreaming(
                sessionStore.currentSession(),
                trimmed,
                modelOverride,
                delta -> {
                    writer.print(delta);
                    writer.flush();
                    streamedReply.append(delta);
                }
        );
        sessionStore.replace(turn.sessionContext());
        renderTurn(writer, turn, streamedReply.toString());
        writer.flush();
        return true;
    }

    LineReader createLineReader(Terminal terminal) {
        Path historyPath = resolveHistoryPath();
        createParentDirectories(historyPath);

        DefaultHistory history = new DefaultHistory();
        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .appName("axercode")
                .history(history)
                .highlighter(new DefaultHighlighter())
                .completer(new StringsCompleter(
                        "/help",
                        "/history",
                        "/new",
                        "/status",
                        "/plan",
                        "/focus",
                        "/checkpoint",
                        "/checkpoints",
                        "/diff",
                        "/branch",
                        "/restore",
                        "/exit"
                ))
                .variable(LineReader.HISTORY_FILE, historyPath)
                .build();
        lineReader.setOpt(LineReader.Option.HISTORY_IGNORE_DUPS);
        lineReader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);
        return lineReader;
    }

    private Terminal buildSystemTerminal() throws IOException {
        if (System.console() == null) {
            return TerminalBuilder.builder()
                    .system(false)
                    .streams(System.in, System.out)
                    .dumb(true)
                    .encoding(StandardCharsets.UTF_8)
                    .build();
        }

        return TerminalBuilder.builder()
                .system(true)
                .encoding(StandardCharsets.UTF_8)
                .build();
    }

    private Path resolveHistoryPath() {
        String configuredPath = shellProperties.getHistoryFile();
        String resolvedPath = configuredPath.replace("${user.home}", System.getProperty("user.home"));
        return Path.of(resolvedPath);
    }

    private void createParentDirectories(Path historyPath) {
        try {
            if (historyPath.getParent() != null) {
                Files.createDirectories(historyPath.getParent());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to create shell history directory", exception);
        }
    }

    private void saveHistory(LineReader lineReader) {
        try {
            lineReader.getHistory().save();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to save shell history", exception);
        }
    }

    private void printToolFeedback(PrintWriter writer, CliChatTurn turn) {
        for (String block : toolFeedbackFormatter.formatBlocks(turn.toolResults())) {
            writer.println(block);
        }
    }

    private void renderTurn(PrintWriter writer, CliChatTurn turn, String streamedReply) {
        printToolFeedback(writer, turn);
        if (streamedReply.equals(turn.reply())) {
            writer.println();
            return;
        }
        writer.println(turn.reply());
    }
}
