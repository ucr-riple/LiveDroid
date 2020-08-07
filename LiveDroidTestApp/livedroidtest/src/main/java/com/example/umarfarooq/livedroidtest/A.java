package com.example.umarfarooq.livedroidtest;

public class A {
  private B b;
  private B bb;
  private C c;

  void test() {
    b = bb;
  }

  public B getBb() {
    return bb;
  }

  public void setBb(B bb) {
    this.bb = bb;
  }

  public C getC() {
    return c;
  }

  public void setC(C c) {
    this.c = c;
  }

  public B getB() {
    return b;
  }

  public void setB(B b) {
    this.b = b;
  }

  public A() {
    b = new B();
    c = new C();
    bb = new B();
  }
}
