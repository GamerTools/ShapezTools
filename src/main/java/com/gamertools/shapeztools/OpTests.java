package com.gamertools.shapeztools;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * OpTests
 */
enum OpTests {
  MIRROR_01(Ops::mirrorValue, 0x1234, 0x84c2) {},
  MIRROR_02(Ops::mirrorValue, 0x12345678, 0x84c2a6e1) {},

  KEY_01(Ops::keyValue, 0x4321, 0x1624) {},
  KEY_02(Ops::keyValue, 0x10000000, 0x10000000) {},
  KEY_03(Ops::keyValue, 0x87654321, 0x1e6a2c48) {},

  ROTATE_01(Ops::rotateRight, 0x0001, 0x0002) {},
  ROTATE_02(Ops::rotate180, 0x0001, 0x0004) {},
  ROTATE_03(Ops::rotateLeft, 0x0001, 0x0008) {},
  ROTATE_04(Ops::rotateLeft, 0x1248, 0x8124) {},
  ROTATE_05(Ops::rotateLeft, 0x0001001e, 0x00080087) {},

  CUT_01(Ops::cutLeft, 0x936c, 0x00cc) {},
  CUT_02(Ops::cutRight, 0x936c, 0x0132) {},
  CUT_03(Ops::cutLeft, 0x000f0000, 0x000c0000) {},
  CUT_04(Ops::cutRight, 0x000f0000, 0x00030000) {},
  CUT_05(Ops::cutLeft, 0x000f000f, 0x0000) {},
  CUT_06(Ops::cutRight, 0x000f000f, 0x0000) {},
  CUT_07(Ops::cutLeft, 0xe8c4f8c4, 0x0000) {},
  CUT_08(Ops::cutRight, 0xe8c4f8c4, 0x0001) {},
  CUT_09(Ops::cutLeft, 0x00500073, 0x0000) {},
  CUT_10(Ops::cutRight, 0x00500073, 0x00100033) {},
  CUT_11(Ops::cutLeft, 0x005e00ff, 0x0008) {},
  CUT_12(Ops::cutRight, 0x005e00ff, 0x00100031) {},

  PIN_01(Ops::pinPush, 0x0001, 0x00010010) {},
  PIN_02(Ops::pinPush, 0x00030030, 0x00330300) {},
  PIN_03(Ops::pinPush, 0xf931, 0x00019310) {},
  PIN_04(Ops::pinPush, 0x11701571, 0x00010014) {},
  PIN_05(Ops::pinPush, 0xcacfffff, 0x000f0170) {},
  PIN_06(Ops::pinPush, 0x22222273, 0x00030114) {},

  CRYSTAL_01(Ops::crystal, 0x0001, 0x000e000f) {},
  CRYSTAL_02(Ops::crystal, 0x00010010, 0x00ef00ff) {},
  CRYSTAL_03(Ops::crystal1, 0x0001, 0x0000) {},
  CRYSTAL_04(Ops::crystal1, 0x0000000e, 0x0001000f) {},

  SWAP_01(Ops::swapLeft, 0x000f, 0x000f, 0x000f) {},
  SWAP_02(Ops::swapRight, 0x000f, 0x000f, 0x000f) {},
  SWAP_03(Ops::swapLeft, 0x0009, 0x0006, 0x0005) {},
  SWAP_04(Ops::swapRight, 0x0009, 0x0006, 0x000a) {},

  STACK_01(Ops::stack, 0xfffa, 0x5111, 0x511b) {},
  STACK_02(Ops::stack, 0x00010000, 0x1111, 0x1111) {},
  STACK_03(Ops::stack, 0x000f, 0x00010000, 0x000100f0) {},
  STACK_04(Ops::stack, 0x00100001, 0x00010110, 0x00011110) {},
  STACK_05(Ops::stack, 0x000f0000, 0x08ce, 0x842108ce) {},
  STACK_06(Ops::stack, 0x000f005f, 0x000a, 0x0000000f) {};

  IntUnaryOperator f1;
  IntBinaryOperator f2;
  int value1, value2, result;

  OpTests(IntUnaryOperator func, int value1, int result) {
    this.f1 = func;
    this.value1 = value1;
    this.result = result;
  }

  OpTests(IntBinaryOperator func, int value1, int value2, int result) {
    this.f2 = func;
    this.value1 = value1;
    this.value2 = value2;
    this.result = result;
  }

  static void run() {
    System.out.println("Run OpTests...");
    int result;
    boolean pass;
    for (OpTests t : OpTests.values()) {
      if (t.f1 != null) {
        result = t.f1.applyAsInt(t.value1);
      } else if (t.f2 != null) {
        result = t.f2.applyAsInt(t.value1, t.value2);
      } else {
        System.out.printf("ERROR: test %s missing function\n", t.name());
        continue;
      }
      pass = (result == t.result);
      System.out.printf("%s %s\n", pass ? "PASS" : "FAIL", t.name());
      if (!pass) {
        System.out.printf("  returned %08x, expected %08x\n", result, t.result);
      }
    }
    System.out.println();
  }
}
