package com.gamertools.shapeztools;

import java.util.Set;

/**
 * Tools
 */
public class Tools {

  static void displayShapes(int[] shapes) {
    if (shapes.length == 0) {
      System.out.println("No shapes to display");
      return;
    }
    for (int shape : shapes) {
      System.out.println(new Shape(shape));
    }
    System.out.printf("Number of shapes: %d\n", shapes.length);
    System.out.println();
  }

  static void displayShapes(Set<Integer> shapes) {
    int shape;
    System.out.println("All shapes");
    for (long i = 0; i <= 0xffffffffl; ++i) {
      shape = (int) i;
      if (shapes.contains(shape))
        System.out.println(new Shape(shape));
    }
    System.out.println();
  }

}
