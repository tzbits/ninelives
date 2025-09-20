package com.tzbits.ninelives;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class NineLives {

  @Parameter
  private List<String> parameters = new ArrayList<>();

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
      System.err.printf(e.getMessage() + "\n");
      System.exit(-1);
    }
    nineLives.run();
  }

  private void run() {
    try {
      Files.write(
          Path.of(outputFileName),
          Transpiler.forFile(Path.of(inputFileName)).transpile().getBytes(),
          StandardOpenOption.CREATE,
          StandardOpenOption.WRITE);
    } catch (IOException e) {
      System.err.printf(e.getMessage() + "\n");
      if (debug) {
        e.printStackTrace();
      }
      System.exit(-1);
    } catch (IllegalStateException e) {
      if (debug) {
        e.printStackTrace();
      }
      System.exit(-1);
    } catch (Exception e) {
      System.err.printf(e.getMessage() + "\n");
      e.printStackTrace();
    }
  }
}
