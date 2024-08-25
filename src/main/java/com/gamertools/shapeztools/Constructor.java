package com.gamertools.shapeztools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

/**
 * Constructor
 */
public class Constructor {
  private static final int MAX_ITERS = 100;
  private static final int MAX_LAYERS = 3;
  private static final int BATCH_SIZE = 10000000;

  final String RESULTS = "BigData/shapes.txt";

  private static final IntUnaryOperator[] ONE_OPS_ALL = { Ops::rotateRight, Ops::rotate180, Ops::rotateLeft,
      Ops::cutLeft, Ops::cutRight, Ops::pinPush, Ops::crystal };
  private static final IntBinaryOperator[] TWO_OPS_ALL = { Ops::swapLeft, Ops::swapRight, Ops::stack };
  private static final IntUnaryOperator[] ONE_OPS = { Ops::rotateRight, Ops::cutRight, Ops::pinPush, Ops::crystal };
  private static final IntBinaryOperator[] TWO_OPS = { Ops::fastSwapRight, Ops::fastStack };

  private Set<Integer> allShapes = new HashSet<>();
  private Set<Integer> newShapes = Collections.synchronizedSet(new HashSet<>());

  private IntStream shapeStream(Set<Integer> shapes) {
    return shapes.stream().mapToInt(Integer::intValue);
  }

  private IntStream shapeStream(int[] shapes) {
    return Arrays.stream(shapes);
  }

  Set<Integer> takeValues(Set<Integer> srcSet, int maxValues) {
    Set<Integer> dstSet = new HashSet<>();
    for (int v : srcSet) {
      if (maxValues-- == 0)
        break;
      dstSet.add(v);
    }
    srcSet.removeAll(dstSet);
    return dstSet;
  }

  private boolean maxLayers(int shape) {
    if (shape == 0)
      return false;
    return (Shape.v1(shape) | Shape.v2(shape)) < (1 << (4 * MAX_LAYERS));
  }

  private boolean oneLayerNoCrystal(int shape) {
    return Shape.isOneLayer(shape) && !Shape.hasCrystal(shape);
  }

  void run() {
    // int[] shapes = Arrays.stream(Shape.FLAT_4).toArray();
    int[] shapes = Arrays.stream(new int[][] { Shape.FLAT_4, Shape.PIN_4 }).flatMapToInt(Arrays::stream).toArray();

    System.out.println("Max iters: " + MAX_ITERS);
    System.out.println("Max layers: " + MAX_LAYERS);
    System.out.println("Batch size: " + BATCH_SIZE);
    System.out.println("Input shapes");
    Tools.displayShapes(shapes);

    Arrays.stream(shapes).forEach(newShapes::add);

    Set<Integer> inputShapes;
    for (int i = 1; i <= MAX_ITERS; ++i) {
      System.out.printf("ITER #%d\n", i);
      inputShapes = takeValues(newShapes, BATCH_SIZE);
      /* TODO: add inputShapes to allShapes before calling makeShapes */
      makeShapes(inputShapes);
      allShapes.addAll(inputShapes);

      if (newShapes.size() > 0) {
        System.out.printf("TODO %d\n\n", newShapes.size());
      } else {
        System.out.printf("DONE\n\n");
        break;
      }
    }
  }

  /**
   * makeShapes
   * 
   * Given a list of starting shapes, find the shapes that can be made by performing all operations.
   */
  void makeShapes(Set<Integer> inputShapes) {
    List<IntStream> streams = new ArrayList<>();
    IntStream stream;
    int inputLen = inputShapes.size();

    System.out.printf("ONE_OPS %d %d > %d\n", ONE_OPS.length, inputLen, 1l * ONE_OPS.length * inputLen);
    for (IntUnaryOperator op : ONE_OPS) {
      streams.add(shapeStream(inputShapes).map(op));
    }

    System.out.printf("TWO_OPS %d %d %d > %d\n", TWO_OPS.length, inputLen, allShapes.size(),
        1l * TWO_OPS.length * ((1l * inputLen * inputLen) + (2l * inputLen * allShapes.size())));
    makeStreams(streams, inputShapes, Ops.Name.FAST_SWAP, x -> Shape.isLeftHalf(x), x -> Shape.isRightHalf(x));
    makeStreams(streams, inputShapes, Ops.Name.STACK, this::oneLayerNoCrystal, x -> true);

    stream = streams.parallelStream().flatMapToInt(s -> s);
    stream = stream.filter(s -> this.maxLayers(s));
    stream = stream.filter(s -> !allShapes.contains(s));
    stream = stream.filter(s -> !inputShapes.contains(s));
    // stream = stream.filter(s -> !newShapes.contains(s));
    stream.forEach(s -> newShapes.add(s));
  }

  /* This "completes the square" by doing all operations that have not been done before. */
  void makeStreams(List<IntStream> streams, Set<Integer> inputShapes, Ops.Name opName, IntPredicate pre1,
      IntPredicate pre2) {
    int[] set1 = shapeStream(inputShapes).filter(pre1).toArray();
    int[] set2 = shapeStream(inputShapes).filter(pre2).toArray();
    streams.add(shapeStream(allShapes).filter(pre2).mapMulti((s2, consumer) -> {
      for (int s1 : set1)
        consumer.accept(Ops.invoke(opName, s1, s2));
    }));
    streams.add(shapeStream(allShapes).filter(pre1).mapMulti((s1, consumer) -> {
      for (int s2 : set2)
        consumer.accept(Ops.invoke(opName, s1, s2));
    }));
    streams.add(shapeStream(set1).mapMulti((s1, consumer) -> {
      for (int s2 : set2)
        consumer.accept(Ops.invoke(opName, s1, s2));
    }));
  }

  void displayResults() {
    int[] lefts = shapeStream(allShapes).filter(Shape::isLeftHalf).toArray();
    int[] rights = shapeStream(allShapes).filter(Shape::isRightHalf).toArray();
    int[] noCrystal = shapeStream(allShapes).filter(v -> !Shape.hasCrystal(v)).sorted().toArray();
    int[] oneLayerNoCrystal = shapeStream(allShapes).filter(Shape::isOneLayer).filter(v -> !Shape.hasCrystal(v))
        .toArray();

    System.out.printf("lefts: %d, rights: %d\n", lefts.length, rights.length);
    System.out.printf("noCrystal: %d\n", noCrystal.length);
    System.out.printf("oneLayerNoCrystal: %d\n", oneLayerNoCrystal.length);
    System.out.printf("Number: %d\n", allShapes.size());
  }

  void saveResults() {
    ShapeFile.write(RESULTS, allShapes);
  }

  void shutdown() {
    saveResults();
  }

}
