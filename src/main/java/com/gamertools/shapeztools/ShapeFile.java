package com.gamertools.shapeztools;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * ShapeFile
 */
public class ShapeFile {

  static void write(String name, int[] data) {
    Set<Integer> dataSet = IntStream.of(data).boxed().collect(Collectors.toSet());
    write(name, dataSet, false);
  }

  static void write(String name, Set<Integer> data) {
    write(name, data, false);
  }

  static void append(String name, int[] data) {
    Set<Integer> dataSet = IntStream.of(data).boxed().collect(Collectors.toSet());
    write(name, dataSet, true);
  }

  static void append(String name, Set<Integer> data) {
    write(name, data, true);
  }

  static void writeDB(String name, Map<Integer, Solver.Build> data) {
    writeDB(name, data, false);
  }

  static void appendDB(String name, Map<Integer, Solver.Build> data) {
    writeDB(name, data, true);
  }

  static void delete(String name) {
    try {
      Files.delete(Path.of(name));
    } catch (Exception e) {
      System.err.printf("Error deleting file: %s\n", name);
      e.printStackTrace();
    }
  }

  static Set<Integer> read(String name) {
    final int SIZE = 350000000;
    Set<Integer> result = new HashSet<>(SIZE);

    /* Get list of files */
    Path file = Paths.get(name);
    if (!Files.isRegularFile(file)) {
      System.err.printf("Unknown file: %s\n", name);
      return null;
    }

    System.out.printf("Reading file: %s\n", file);
    try (Stream<String> lines = Files.lines(file)) {
      lines.forEach(line -> {
        String[] values = line.split(",");
        int shape = Integer.parseUnsignedInt(values[0], 16);
        result.add(shape);
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
    // break;
    System.out.printf("number of shapes: %d\n", result.size());

    return result;
  }

  static void write(String name, Set<Integer> data, boolean append) {
    final int BIG_DATA_SIZE = 10000000;
    System.out.printf("Writing file: %s\n", name);
    try (FileWriter file = new FileWriter(name, append)) {
      PrintWriter out = new PrintWriter(file);
      if (data.size() < BIG_DATA_SIZE)
        writeFast(out, data);
      else
        writeSlow(out, data);
    } catch (Exception e) {
      System.err.printf("Error writing file: %s\n", name);
      e.printStackTrace();
    }
  }

  private static void writeFast(PrintWriter out, Set<Integer> data) {
    Integer[] shapes = data.toArray(Integer[]::new);
    Arrays.parallelSort(shapes, (x, y) -> Integer.compareUnsigned(x, y));
    for (Integer shape : shapes)
      out.printf("%08x\n", shape);
  }

  private static void writeSlow(PrintWriter out, Set<Integer> data) {
    int shape;
    for (long i = 0; i <= 0xffffffffl; ++i) {
      shape = (int) i;
      if (data.contains(shape))
        out.printf("%08x\n", shape);
    }
  }

  static void sort(String name) {
    System.out.printf("Sorting file: %s\n", name);
    Map<Integer, Solver.Build> dataMap = readMultiDB(name);
    writeDB(name, dataMap, false);
  }

  static class Build {
    int cost;
    String opCode;
    int shape1, shape2;
  }

  static Map<Integer, Solver.Build> readMultiDB(String name) {
    final int SIZE = 350000000;
    Map<Integer, Solver.Build> result = new HashMap<>(SIZE);

    /* Get list of files */
    Path path = Paths.get(name);
    List<Path> files = new ArrayList<>();
    if (Files.isRegularFile(path)) {
      files.add(path);
    } else if (Files.isDirectory(path)) {
      try (Stream<Path> stream = Files.list(path)) {
        files = stream.filter(file -> !Files.isDirectory(file)).collect(Collectors.toList());
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      System.err.printf("Unknown file: %s\n", name);
    }
    System.out.println("number of files: " + files.size());

    /* Read all files */
    for (Path file : files) {
      System.out.printf("Reading file: %s\n", file);
      try (Stream<String> lines = Files.lines(file)) {
        lines.forEach(line -> {
          String[] values = line.split(",");
          int shape = Integer.parseUnsignedInt(values[0], 16);
          Ops.Name opName = Ops.nameByCode.get(values[1]);
          int shape1 = Integer.parseUnsignedInt(values[2], 16);
          int shape2 = Integer.parseUnsignedInt(values[3], 16);
          int cost = Integer.parseUnsignedInt(values[4], 16);
          result.put(shape, new Solver.Build(cost, opName, shape, shape1, shape2));
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    System.out.printf("number of builds: %d\n", result.size());

    return result;
  }

  static void writeMultiDB(String dirName, Map<Integer, Solver.Build> builds) {
    final String PREFIX = "SDB";
    Path dirPath = Paths.get(dirName);
    if (!Files.isDirectory(dirPath)) {
      System.out.printf("filename is not a directory: %s\n", dirName);
    }
    // for all index values 00..ff
    for (int index = 0; index <= 0xff; ++index) {
      String filename = dirPath.resolve(String.format("%s%02x", PREFIX, index)).toString();
      System.out.printf("Writing file: %s\n", filename);
      int init1, init2;
      int shape;
      Solver.Build build;
      try (FileWriter file = new FileWriter(filename)) {
        PrintWriter out = new PrintWriter(file);
        init1 = (index & 0x0f) << 12;
        init2 = (index & 0xf0) << 8;
        for (int v2 = init2; v2 <= init2 + 0xfff; ++v2) {
          for (int v1 = init1; v1 <= init1 + 0xfff; ++v1) {
            shape = (v2 << 16) | v1;
            // System.out.printf("%s %08x\n", filename, shape);
            build = builds.get(shape);
            if (build != null)
              out.print(buildAsString(build));
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static String buildAsString(Solver.Build build) {
    return String.format("%08x,%s,%08x,%08x,%02x\n", build.shape, build.opName.code, build.shape1, build.shape2,
        build.cost);
  }

  static void writeDB(String name, Map<Integer, Solver.Build> data, boolean append) {
    final int BIG_DATA_SIZE = 10000000;
    System.out.printf("Writing file: %s\n", name);
    try (FileWriter file = new FileWriter(name, append)) {
      PrintWriter out = new PrintWriter(file);
      if (data.size() < BIG_DATA_SIZE)
        writeDBFast(out, data);
      else
        writeDBSlow(out, data);
    } catch (Exception e) {
      System.err.printf("Error writing file: %s\n", name);
      e.printStackTrace();
    }
  }

  private static void writeDBFast(PrintWriter out, Map<Integer, Solver.Build> data) {
    Integer[] shapes = data.keySet().toArray(Integer[]::new);
    Arrays.parallelSort(shapes, (x, y) -> Integer.compareUnsigned(x, y));
    for (Integer shape : shapes)
      out.print(buildAsString(data.get(shape)));
  }

  private static void writeDBSlow(PrintWriter out, Map<Integer, Solver.Build> data) {
    Solver.Build build;
    for (long i = 0; i <= 0xffffffffl; ++i) {
      build = data.get((int) i);
      if (build != null)
        out.print(buildAsString(build));
    }
  }

}
