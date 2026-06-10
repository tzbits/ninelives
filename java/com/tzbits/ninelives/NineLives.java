package com.tzbits.ninelives;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class NineLives {

  @Parameter(names = "--in",
             description = "The name of the input file.",
             required = true)
  private String inputFileName;

  @Parameter(names = "--out",
             description = "The name of the output file.",
             required = true)
  private String outputFileName;

  @Parameter(names = "--debug", description = "Whether to print a stack trace.")
  private boolean debug = false;

  public static void main(String[] argv) {
    NineLives nineLives = new NineLives();
    try {
      JCommander.newBuilder()
          .addObject(nineLives)
          .build()
          .parse(argv);
    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      System.exit(-1);
    }
    nineLives.run();
  }

  private void run() {
    try {
      Files.writeString(
          Path.of(outputFileName),
          Transpiler.forFile(Path.of(inputFileName)).transpile(),
          StandardOpenOption.CREATE,
          StandardOpenOption.WRITE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      System.err.println(e.getMessage());
      if (debug) {
          //noinspection CallToPrintStackTrace
          e.printStackTrace();
      }
      System.exit(-1);
    } catch (IllegalStateException e) {
      // fatalError already printed the message with its line number.
      if (debug) {
        //noinspection CallToPrintStackTrace
        e.printStackTrace();
      }
      System.exit(-1);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
