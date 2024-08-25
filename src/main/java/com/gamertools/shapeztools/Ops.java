package com.gamertools.shapeztools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ops
 */
class Ops {

  enum Stats {
    CUT, SWAP, STACK, ROTATE, PINPUSH, CRYSTAL, COLLAPSE;

    AtomicLong value = new AtomicLong();

    static void clear() {
      for (Stats t : Stats.values())
        t.value.set(0);
    }

    void increment() {
      value.incrementAndGet();
    }

    static String asString() {
      return String.format("cut: %d, swap: %d, stack: %d, rotate: %d, pinPush %d, crystal %d, collapse: %d\n",
          CUT.value.get(), SWAP.value.get(), STACK.value.get(), ROTATE.value.get(), PINPUSH.value.get(),
          CRYSTAL.value.get(), COLLAPSE.value.get());
    }
  }

  enum Name {
    NOP("--") {},
    ROTATE_RIGHT("RR") {},
    ROTATE_180("RU") {},
    ROTATE_LEFT("RL") {},
    CUT_RIGHT("CR") {},
    CUT_LEFT("CL") {},
    PINPUSH("PP") {},
    CRYSTAL("XX") {},
    SWAP_RIGHT("SR") {},
    SWAP_LEFT("SL") {},
    FAST_SWAP("SW") {},
    STACK("ST") {};

    String code;

    Name(String code) {
      this.code = code;
      nameByCode.put(code, this);
    }
  }

  static Map<String, Name> nameByCode = new HashMap<>();

  static int invoke(Name opName, int... shapes) {
    switch (opName) {
    case NOP:
      return nop(shapes[0]);
    case ROTATE_RIGHT:
      return rotateRight(shapes[0]);
    case ROTATE_180:
      return rotate180(shapes[0]);
    case ROTATE_LEFT:
      return rotateLeft(shapes[0]);
    case CUT_RIGHT:
      return cutRight(shapes[0]);
    case CUT_LEFT:
      return cutLeft(shapes[0]);
    case PINPUSH:
      return pinPush(shapes[0]);
    case CRYSTAL:
      return crystal(shapes[0]);
    case SWAP_RIGHT:
      return swapRight(shapes[0], shapes[1]);
    case SWAP_LEFT:
      return swapLeft(shapes[0], shapes[1]);
    case FAST_SWAP:
      return fastSwap(shapes[0], shapes[1]);
    case STACK:
      return stack(shapes[0], shapes[1]);
    default:
      System.err.println("Unkown op name: " + opName.name());
      return 0;
    }
  }

  /**
   * Unsigned min()
   * 
   * @param x
   * @param y
   * @return Integer value
   */
  private static int umin(int x, int y) {
    return Integer.compareUnsigned(x, y) < 0 ? x : y;
  }

  /**
   * Compute the shape's key value.
   * 
   * @param shape
   * @return Key value
   */
  static int keyValue(int shape) {
    int mvalue = mirrorValue(shape);
    int result = umin(shape, mvalue);

    for (int i = 1; i < 4; ++i) {
      result = umin(result, rotate(shape, i));
      result = umin(result, rotate(mvalue, i));
    }
    return result;
  }

  /**
   * Compute the value of the shape's mirror image.
   * 
   * @param shape
   * @return Value of the shape's mirror image.
   */
  static int mirrorValue(int shape) {
    int result = 0;
    for (int i = 0; i < 4; ++i) {
      result = (result << 1) | (shape & 0x11111111);
      shape >>>= 1;
    }
    return result;
  }

  /**
   * Identity operation.
   * 
   * @param shape
   * @return
   */
  static int nop(int shape) {
    return shape;
  }

  /**
   * Rotate the shape to the right a given number of steps.
   * 
   * @param shape
   * @param steps
   * @return Value of the rotated shape
   */
  static int rotate(int shape, int steps) {
    Stats.ROTATE.increment();
    int lShift = steps & 0x3;
    int rShift = 4 - lShift;
    int mask = (0xf >>> rShift) * 0x11111111;
    int result = ((shape >>> rShift) & mask) | ((shape << lShift) & ~mask);
    return result;
  }

  static int rotateRight(int shape) {
    return rotate(shape, 1);
  }

  static int rotate180(int shape) {
    return rotate(shape, 2);
  }

  static int rotateLeft(int shape) {
    return rotate(shape, 3);
  }

  /**
   * Drop a part on top of a shape. The part is assumed to be a single solid part that won't separate.
   * 
   * @param base
   * @param part
   * @param layerNum starting layer number
   * @return
   */
  private static int dropPart(int base, int part, int layerNum) {
    if (part == 0)
      return base;
    int v1 = Shape.v1(base);
    int v2 = Shape.v2(base);
    int value = v1 | v2;
    for (int offset = layerNum; offset > 0; --offset) {
      if (((part << (4 * (offset - 1))) & value) != 0) {
        return base | ((part << (4 * offset)) & 0xffff);
      }
    }
    return base | part;
  }

  /**
   * Drop a pin on top of a shape.
   * 
   * TODO: Only need the quad/column of the pin. Find top zero.
   * 
   * @param base
   * @param quad
   * @param layerNum starting layer number
   * @return
   */
  private static int dropPin(int base, int quad, int layerNum) {
    int pin = 1 << quad;
    int v1 = Shape.v1(base);
    int v2 = Shape.v2(base);
    int value = v1 | v2;
    for (int offset = layerNum; offset > 0; --offset) {
      if (((pin << (4 * (offset - 1))) & value) != 0) {
        return base | (Shape.PIN_MASK << (4 * offset + quad));
      }
    }
    return base | (Shape.PIN_MASK << quad);
  }

  private static final int[][] NEXT_SPOTS2 = { { 1, 4 }, { 0, 5 }, { 3, 6 }, { 2, 7 }, { 0, 5, 8 }, { 1, 4, 9 },
      { 2, 7, 10 }, { 3, 6, 11 }, { 4, 9, 12 }, { 5, 8, 13 }, { 6, 11, 14 }, { 7, 10, 15 }, { 8, 13 }, { 9, 12 },
      { 10, 15 }, { 11, 14 } };

  private static final int[][] NEXT_SPOTS4 = { { 1, 3, 4 }, { 0, 2, 5 }, { 1, 3, 6 }, { 0, 2, 7 }, { 0, 5, 7, 8 },
      { 1, 4, 6, 9 }, { 2, 5, 7, 10 }, { 3, 4, 6, 11 }, { 4, 9, 11, 12 }, { 5, 8, 10, 13 }, { 6, 9, 11, 14 },
      { 7, 8, 10, 15 }, {}, {}, {}, {} };

  /**
   * @param shape
   * @param todo
   * @param mesh
   * @return
   */
  private static int findCrystals(int shape, List<Integer> todo, int[][] mesh) {
    int result = 0;
    int num, val;
    for (int i = 0; i < todo.size(); ++i) {
      num = todo.get(i);
      result |= Shape.CRYSTAL_MASK << num;
      for (int spot : mesh[num]) {
        if (todo.contains(spot))
          continue;
        val = (shape >>> spot) & Shape.CRYSTAL_MASK;
        if (val == Shape.CRYSTAL_MASK)
          todo.add(spot);
      }
    }
    return result;
  }

  private static int findCrystals_new(int shape, List<Integer> todo, int[][] mesh) {
    int result = 0;
    int num, val;
    boolean[] todo2 = new boolean[Shape.NUM_SPOTS];
    for (int i = 0; i < todo.size(); ++i)
      todo2[todo.get(i)] = true;
    for (int i = 0; i < todo.size(); ++i) {
      num = todo.get(i);
      result |= Shape.CRYSTAL_MASK << num;
      for (int spot : mesh[num]) {
        if (todo2[spot])
          continue;
        val = (shape >>> spot) & Shape.CRYSTAL_MASK;
        if (val == Shape.CRYSTAL_MASK) {
          todo.add(spot);
          todo2[spot] = true;
        }
      }
    }
    return result;
  }

  /**
   * TODO: Use spot number instead of layers and quads.
   * 
   * @param shape
   * @param quads
   * @return
   */
  private static int collapse(int shape, int[] quads) {
    Stats.COLLAPSE.increment();
    int part, val;
    int prevLayer;
    int v1, v2;
    boolean supported;

    // First layer remains unchanged
    int result = shape & Shape.LAYER_MASK;

    // int[] layerNums = new int[]{1, 2, 3};
    // IntStream.range(1, 4).forEach(layerNum -> ...)
    for (int layerNum = 1; layerNum < Shape.NUM_LAYERS; ++layerNum) {
      part = (shape >>> (4 * layerNum)) & Shape.LAYER_MASK;
      if (part == 0)
        continue;
      // Drop all pins
      for (int quad : quads) {
        val = (part >>> quad) & Shape.CRYSTAL_MASK;
        if (val == Shape.PIN_MASK) {
          part &= ~(Shape.PIN_MASK << quad);
          result = dropPin(result, quad, layerNum);
        }
      }

      // find parts
      List<Integer> parts = new ArrayList<>(2);
      v1 = Shape.v1(part);
      if (v1 == 0x5) {
        parts.add(part & (Shape.CRYSTAL_MASK << 0));
        parts.add(part & (Shape.CRYSTAL_MASK << 2));
      } else if (v1 == 0xa) {
        parts.add(part & (Shape.CRYSTAL_MASK << 1));
        parts.add(part & (Shape.CRYSTAL_MASK << 3));
      } else {
        parts.add(part);
      }

      // Check if part is supported
      prevLayer = (result >>> (4 * (layerNum - 1))) & Shape.LAYER_MASK;
      prevLayer = Shape.v1(prevLayer) | Shape.v2(prevLayer);
      for (int part1 : parts) {
        supported = (part1 & prevLayer) != 0;
        if (supported) {
          // copy part
          result |= part1 << (4 * layerNum);
          continue;
        }
        // break crystals
        v2 = Shape.v2(part1);
        part1 &= ~(v2 * Shape.CRYSTAL_MASK);
        if (part1 == 0)
          continue;
        // drop parts
        // Note: Need to do this again because it might be two parts after breaking
        // crystals.
        if (part1 == 0x5) {
          result = dropPart(result, 0x1, layerNum);
          result = dropPart(result, 0x4, layerNum);
        } else if (part1 == 0xa) {
          result = dropPart(result, 0x2, layerNum);
          result = dropPart(result, 0x8, layerNum);
        } else {
          result = dropPart(result, part1, layerNum);
        }
      }

    }
    return result;
  }

  static int cutLeft(int shape) {
    Stats.CUT.increment();
    int[] layers = Shape.toLayers(shape);
    // Step 1: break all cut crystals
    // Check all 8 places that a crystal can span the cut
    int layer;
    List<Integer> todo = new ArrayList<>();
    for (int layerNum = 0; layerNum < layers.length; ++layerNum) {
      layer = layers[layerNum];
      if ((layer & 0x99) == 0x99)
        todo.add(4 * layerNum + 3);
      if ((layer & 0x66) == 0x66)
        todo.add(4 * layerNum + 2);
    }
    // Find all connected crystals
    int found = findCrystals(shape, todo, NEXT_SPOTS2);
    // Break all connected crystals
    shape &= ~found;

    // Step 2: Collapse parts
    return collapse(shape & 0xcccccccc, new int[] { 2, 3 });
  }

  static int cutRight(int shape) {
    Stats.CUT.increment();
    int[] layers = Shape.toLayers(shape);
    // Step 1: break all cut crystals
    // Check all 8 places that a crystal can span the cut
    int layer;
    List<Integer> todo = new ArrayList<>();
    for (int layerNum = 0; layerNum < layers.length; ++layerNum) {
      layer = layers[layerNum];
      if ((layer & 0x99) == 0x99)
        todo.add(4 * layerNum + 0);
      if ((layer & 0x66) == 0x66)
        todo.add(4 * layerNum + 1);
    }
    // Find all connected crystals
    int found = findCrystals(shape, todo, NEXT_SPOTS2);
    // Break all connected crystals
    shape &= ~found;

    // Step 2: Collapse parts
    return collapse(shape & 0x33333333, new int[] { 0, 1 });
  }

  static int pinPush(int shape) {
    Stats.PINPUSH.increment();

    // make pins
    int v1 = Shape.v1(shape);
    int v2 = Shape.v2(shape);
    int pins = ((v1 | v2) & 0xf) * Shape.PIN_MASK;

    int result = 0;
    // Step 1: break all cut crystals
    // Check all 4 places that a crystal can span the cut
    List<Integer> todo = new ArrayList<>();
    int spot1, spot2;
    for (int spot = 8; spot < 12; ++spot) {
      // check for crystal at spot and the spot directly above it
      spot1 = (shape >>> spot) & Shape.CRYSTAL_MASK;
      spot2 = (shape >>> (spot + 4)) & Shape.CRYSTAL_MASK;
      if (spot1 == Shape.CRYSTAL_MASK && spot2 == Shape.CRYSTAL_MASK) {
        todo.add(spot);
      }
    }
    // Find all connected crystals
    int found = findCrystals(shape, todo, NEXT_SPOTS4);
    // Break all connected crystals
    shape &= ~found;

    // Step 2: Raise shape and add pins
    v1 = Shape.v1(shape);
    v2 = Shape.v2(shape);
    shape = (v2 << 20) | ((v1 & 0x0fff) << 4);
    shape |= pins;

    // Step 3: Collapse parts
    shape = collapse(shape, new int[] { 0, 1, 2, 3 });

    return shape;
  }

  static int crystal(int shape) {
    Stats.CRYSTAL.increment();
    int numLayers = Shape.layerCount(shape);
    // pins and gaps become crystals
    int mask = 0xf, gaps;
    for (int i = 0; i < numLayers; ++i) {
      gaps = ~shape & mask;
      shape |= gaps * Shape.CRYSTAL_MASK;
      mask <<= 4;
    }
    return shape;
  }

  /* Make at most one crystal quarter */
  static int crystal1(int shape) {
    // make sure that the shape has only one gap.
    int mask = 0x1;
    int gaps = 0;
    int numQuads = Shape.layerCount(shape) * Shape.NUM_QUADS;
    // pins and gaps become crystals
    for (int i = 0; i < numQuads; ++i) {
      if ((shape & mask) == 0)
        ++gaps;
      mask <<= 1;
    }
    if (gaps == 1)
      return crystal(shape);
    else
      return 0;
  }

  static int swapLeft(int left, int right) {
    Stats.SWAP.increment();
    int leftHalf = cutLeft(right);
    int rightHalf = cutRight(left);
    return leftHalf | rightHalf;
  }

  static int swapRight(int left, int right) {
    Stats.SWAP.increment();
    int leftHalf = cutLeft(left);
    int rightHalf = cutRight(right);
    return leftHalf | rightHalf;
  }

  static int fastSwapRight(int left, int right) {
    // if (!Shape.isLeftHalf(left) || !Shape.isRightHalf(right))
    // return 0;
    Stats.SWAP.increment();
    return left | right;
  }

  static int fastSwap(int left, int right) {
    Stats.SWAP.increment();
    return left | right;
  }

  static int stack_old(int top, int bottom) {
    Stats.STACK.increment();
    int val;
    int[] layers = Shape.toLayers(top);
    for (int part : layers) {
      if (part == 0)
        break;
      for (int quad = 0; quad < Shape.NUM_QUADS; ++quad) {
        val = (part >>> quad) & 0x11;
        if (val == 0x10) {
          // drop pin
          part &= ~(0x10 << quad);
          bottom = dropPin(bottom, quad, Shape.NUM_LAYERS);
        } else if (val == 0x11) {
          // break crystal
          part &= ~(0x11 << quad);
        }
      }
      // check only solids remain
      if (part > 0xf) {
        System.out.println("Stacking error.  Non solid part found.");
        return 0;
      }
      // find all parts
      List<Integer> parts = new ArrayList<>(2);
      if (part == 0x5) {
        parts.add(0x1);
        parts.add(0x4);
      } else if (part == 0xa) {
        parts.add(0x2);
        parts.add(0x8);
      } else {
        parts.add(part);
      }
      // drop parts
      for (int part1 : parts) {
        bottom = dropPart(bottom, part1, Shape.NUM_LAYERS);
      }
    }
    return bottom;
  }

  static int stack(int top, int bottom) {
    Stats.STACK.increment();

    // break all crystals on top
    top &= ~((Shape.v1(top) & Shape.v2(top)) * Shape.CRYSTAL_MASK);

    int part, value;
    for (int layerNum = 0; layerNum < Shape.NUM_LAYERS; ++layerNum) {
      part = (top >>> (4 * layerNum)) & Shape.LAYER_MASK;
      if (part == 0)
        continue;
      // drop pins
      for (int quad = 0; quad < Shape.NUM_QUADS; ++quad) {
        value = (part >>> quad) & Shape.CRYSTAL_MASK;
        if (value == Shape.PIN_MASK) {
          part &= ~(Shape.PIN_MASK << quad);
          bottom = dropPin(bottom, quad, Shape.NUM_LAYERS);
        }
      }
      // drop parts
      if (part == 0x5) {
        bottom = dropPart(bottom, 0x1, Shape.NUM_LAYERS);
        bottom = dropPart(bottom, 0x4, Shape.NUM_LAYERS);
      } else if (part == 0xa) {
        bottom = dropPart(bottom, 0x2, Shape.NUM_LAYERS);
        bottom = dropPart(bottom, 0x8, Shape.NUM_LAYERS);
      } else {
        bottom = dropPart(bottom, part, Shape.NUM_LAYERS);
      }
    }
    return bottom;

  }

  /**
   * fastStack
   * 
   * Only call stack() when the top shape is 1-layer, no crystal.
   */
  static int fastStack(int top, int bottom) {
    if (!Shape.isOneLayer(top) || Shape.hasCrystal(top))
      return 0;
    return stack(top, bottom);
  }

}
