package com.gamertools.shapeztools;

/**
 * A ShapeDB contains builds for a set of shapes.
 * The data may be split into multiple files.
 * The filename format SDBXY contains shapes with int values of XxxxYxxx.
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * ShapeDB
 */
public class ShapeDB {

  private static final String PREFIX = "SDB";
  private Path dirPath;
  // private List<Path> files = new ArrayList<>();
  private Set<Integer> register = new HashSet<>();

  class Build {
    Ops.Name opName;
    int shape1, shape2;

    Build(Ops.Name opName, int shape1, int shape2) {
      this.opName = opName;
      this.shape1 = shape1;
      this.shape2 = shape2;
    }

    public String toString() {
      if (shape1 == 0)
        return "";
      else if (shape2 == 0)
        return String.format("%s(%08x)", opName.code, shape1);
      else
        return String.format("%s(%08x,%08x)", opName.code, shape1, shape2);
    }
  }

  private Map<Integer, Build> builds = new HashMap<>();

  private ShapeDB(Path dirPath) {
    this.dirPath = dirPath;
    // try (Stream<Path> stream = Files.list(dirPath)) {
    // files = stream.filter(file ->
    // !Files.isDirectory(file)).collect(Collectors.toList());
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
  }

  public static ShapeDB open(String dirName) {
    Path dirPath = Paths.get(dirName);
    if (!Files.isDirectory(dirPath)) {
      System.err.printf("Directory not found: %s\n", dirName);
      return null;
    }
    return new ShapeDB(dirPath);
  }

  private int index(int shape) {
    int v1 = (Shape.v1(shape) & 0xf000) >>> 12;
    int v2 = (Shape.v2(shape) & 0xf000) >>> 8;
    return v1 | v2;
  }

  private void readFile(int index) {
    Path file = dirPath.resolve(String.format("%s%02x", PREFIX, index));
    try (Stream<String> lines = Files.lines(file)) {
      lines.forEach(line -> {
        String[] values = line.split(",");
        int shape = Integer.parseUnsignedInt(values[0], 16);
        Ops.Name opName = Ops.nameByCode.get(values[1]);
        int shape1 = Integer.parseUnsignedInt(values[2], 16);
        int shape2 = Integer.parseUnsignedInt(values[3], 16);
        // int cost = Integer.parseUnsignedInt(values[4], 16);
        builds.put(shape, new Build(opName, shape1, shape2));
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Build getBuild(int shape) {
    int index = index(shape);
    if (!register.contains(index)) {
      readFile(index);
      register.add(index);
    }
    return builds.get(shape);
  }
}
