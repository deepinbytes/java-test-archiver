package com.agoda;

import com.agoda.commands.CompressCommand;
import com.agoda.commands.DecompressCommand;
import io.micronaut.configuration.picocli.PicocliRunner;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Entry point of the application
 */
@Command(name = "archiver", description = "Archiver that compresses files/folders",
        mixinStandardHelpOptions = true, subcommands = {CompressCommand.class, DecompressCommand.class})
public class Archiver implements Runnable {

    @Option(names = {"-t", "--test"}, description = "Print test Message")
    boolean verbose;

    public static void main(String[] args) throws Exception {
        PicocliRunner.run(Archiver.class, args);
    }

    public void run() {

        if (verbose) {
            System.out.println("Hi!");
        }
    }
}
