package com.agoda;

import com.agoda.commands.CompressCommand;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompressCommandTest {

    @Test
    public void testWithCommandLineOption() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[] { "/home/jay/Documents", "/home/jay/Downloads/test2", "5" };
            PicocliRunner.run(CompressCommand.class, ctx, args);

            // compress
            assertTrue(baos.toString().contains("Compressing files in directory"));
        }
    }
}
