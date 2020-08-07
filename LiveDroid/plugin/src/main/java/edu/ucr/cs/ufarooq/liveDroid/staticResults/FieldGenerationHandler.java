package edu.ucr.cs.ufarooq.liveDroid.staticResults;

import com.intellij.psi.PsiField;
import java.util.Objects;

public class FieldGenerationHandler {
  private PsiField field;
  private final boolean isConstructorRequired;
  private final boolean isGetterRequired;
  private final boolean isSetterRequired;

  public FieldGenerationHandler(
      PsiField field,
      boolean isConstructorRequired,
      boolean isGetterRequired,
      boolean isSetterRequired) {
    this.field = field;
    this.isConstructorRequired = isConstructorRequired;
    this.isGetterRequired = isGetterRequired;
    this.isSetterRequired = isSetterRequired;
  }

  public PsiField getField() {
    return field;
  }

  public boolean isConstructorRequired() {
    return isConstructorRequired;
  }

  public boolean isGetterRequired() {
    return isGetterRequired;
  }

  public boolean isSetterRequired() {
    return isSetterRequired;
  }

  public boolean requiresGeneration() {
    return isConstructorRequired || isGetterRequired || isSetterRequired;
  }

  public String getMessage() {
    //    if (!requiresGeneration()) {
    //      return null;
    //    }
    StringBuilder message = new StringBuilder();
    if (isConstructorRequired) {
      message.append("Default Constructor for Class: " + field.getContainingClass().getName() + "\n");
    }
    if (isSetterRequired) {
      message.append(
          "Setter for Field \""
              + field.getName()
              + "\" in Class: "
              + field.getContainingClass().getName()
              + "\n");
    }
    if (isGetterRequired) {
      message.append(
          "Getter for Field \""
              + field.getName()
              + "\" in Class: "
              + field.getContainingClass().getName()
              + "\n");
    }
    return message.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FieldGenerationHandler that = (FieldGenerationHandler) o;
    return isConstructorRequired == that.isConstructorRequired
        && isGetterRequired == that.isGetterRequired
        && isSetterRequired == that.isSetterRequired
        && Objects.equals(field, that.field);
  }

  @Override
  public int hashCode() {
    return Objects.hash(field, isConstructorRequired, isGetterRequired, isSetterRequired);
  }
}
