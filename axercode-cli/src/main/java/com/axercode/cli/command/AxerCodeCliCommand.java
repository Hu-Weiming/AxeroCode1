package com.axercode.cli.command;

import com.axercode.cli.service.CliChatService;
import com.axercode.cli.service.CliChatTurn;
import com.axercode.cli.service.InteractiveShellService;
import com.axercode.cli.service.ToolFeedbackFormatter;
import com.axercode.provider.api.ProviderException;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Component
@Command(
        name = "axercode",
        description = "Run a single prompt against the local AxerCode provider stack.",
        version = "AxerCode 0.1.0-SNAPSHOT"
)
public class AxerCodeCliCommand implements Callable<Integer> {

    private final CliChatService cliChatService;
    private final InteractiveShellService interactiveShellService;
    private final ToolFeedbackFormatter toolFeedbackFormatter = new ToolFeedbackFormatter();

    @Spec
    private CommandSpec spec;

    @Option(names = "--prompt", description = "Prompt to send to the configured provider.")
    private String prompt;

    @Option(names = "--model", description = "Optional model override for this one-shot request.")
    private String model;

    @Option(names = "--interactive", description = "Start the in-memory interactive shell.")
    private boolean interactive;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "Show version information and exit.")
    private boolean versionRequested;

    public AxerCodeCliCommand(CliChatService cliChatService, InteractiveShellService interactiveShellService) {
        this.cliChatService = cliChatService;
        this.interactiveShellService = interactiveShellService;
    }

    @Override
    public Integer call() {
        if (interactive) {
            return interactiveShellService.runInteractive(model);
        }

        if (prompt == null || prompt.isBlank()) {
            spec.commandLine().getErr().println("[AxerCode] Either --prompt or --interactive must be provided.");
            return 2;
        }

        try {
            StringBuilder streamedReply = new StringBuilder();
            CliChatTurn turn = cliChatService.askTurnStreaming(prompt, model, delta -> {
                spec.commandLine().getOut().print(delta);
                spec.commandLine().getOut().flush();
                streamedReply.append(delta);
            });
            for (String block : toolFeedbackFormatter.formatBlocks(turn.toolResults())) {
                spec.commandLine().getOut().println(block);
            }
            renderFinalReply(turn, streamedReply.toString());
            return 0;
        } catch (ProviderException exception) {
            spec.commandLine().getErr().println(
                    "[AxerCode] " + exception.providerName() + "/" + exception.operation() + " failed: " + exception.getMessage()
            );
            return 1;
        }
    }

    private void renderFinalReply(CliChatTurn turn, String streamedReply) {
        if (streamedReply.equals(turn.reply())) {
            spec.commandLine().getOut().println();
            return;
        }
        spec.commandLine().getOut().println(turn.reply());
    }
}
