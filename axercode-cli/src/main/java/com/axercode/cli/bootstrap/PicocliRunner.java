package com.axercode.cli.bootstrap;

import com.axercode.cli.command.AxerCodeCliCommand;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

/**
 * Runs the Spring-managed Picocli command as the entrypoint for the console application.
 */
@Component
public class PicocliRunner implements CommandLineRunner, ExitCodeGenerator {

    private final AxerCodeCliCommand rootCommand;
    private final CommandLine.IFactory factory;
    private int exitCode;

    public PicocliRunner(AxerCodeCliCommand rootCommand, CommandLine.IFactory factory) {
        this.rootCommand = rootCommand;
        this.factory = factory;
    }

    @Override
    public void run(String... args) {
        this.exitCode = new CommandLine(rootCommand, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
