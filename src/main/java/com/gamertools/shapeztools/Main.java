package com.gamertools.shapeztools;

import java.util.Date;

/**
 * Main
 */
public class Main {

  static final String VERSION = "Shapez Tools 1.0";

  public static void main(String[] args) {
    System.out.println(VERSION);
    System.out.println(new Date());
    System.out.println();

    // ShapeTests.run();
    // OpTests.run();
    Tests.run();

    // makeShapes();
  }

  static void makeShapes() {
    Ops.Stats.clear();

    // Constructor f = new Constructor();
    Solver f = new Solver();
    Thread exitHook = new Thread(() -> {
      System.out.println("Shutdown");
      f.shutdown();
    });
    Runtime.getRuntime().addShutdownHook(exitHook);

    long before = new Date().getTime();
    f.run();
    long after = new Date().getTime();
    System.out.printf("Time: %d\n", after - before);
    System.out.println(Ops.Stats.asString());

    // Runtime.getRuntime().removeShutdownHook(exitHook);
  }

}
