package com.gamertools.shapeztools;

/**
 * ShapeTests
 */
enum ShapeTests {
  T1("123") {},
  T2("Cu") {},
  EMPTY1("--------", 0) {},
  LOGO1(Shape.LOGO, 0x4b) {},
  ROCKET1(Shape.ROCKET, 0xfe1f) {},
  PINS1("P---P---", 0x00050000) {},
  CRYSTAL1("crP-crP-", 0x000f0005) {},
  CRYSTAL2("cccccccc:cccccccc:cccccccc:cccccccc", 0xffffffff) {};

  boolean valid;
  String code;
  int value;

  ShapeTests(String code) {
    this.valid = false;
    this.code = code;
  }

  ShapeTests(String code, int value) {
    this.valid = true;
    this.code = code;
    this.value = value;
  }

  public static void run() {
    System.out.println("Run ShapeTests...");
    Shape s;
    Boolean pass;
    for (ShapeTests t : ShapeTests.values()) {
      // System.out.println("TEST " + t.code);
      try {
        s = new Shape(t.code);
        pass = (t.valid == true) && (t.value == s.intValue());
      } catch (Exception e) {
        s = null;
        pass = (t.valid == false);
      }
      System.out.printf("%s %s %s\n", pass ? "PASS" : "FAIL", t.name(), s);
      if (!pass && s != null) {
        System.out.printf("  returned %08x, expected %08x\n", s.intValue(), t.value);
      }
    }
    System.out.println();
  }
}