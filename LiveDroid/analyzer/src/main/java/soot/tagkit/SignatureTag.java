package soot.tagkit;

public class SignatureTag implements Tag {
  public static final String NAME = SignatureTag.class.getSimpleName();
  private String signature;

  public SignatureTag(String signature) {
    this.signature = signature;
  }

  public String getSignature() {
    return this.signature;
  }

  public String getName() {
    return NAME;
  }

  public byte[] getValue() {
    throw new RuntimeException(NAME + " has no value for bytecode");
  }

  public String getInfo() {
    return "Signature";
  }

  public String toString() {
    return "Signature: " + this.signature;
  }
}