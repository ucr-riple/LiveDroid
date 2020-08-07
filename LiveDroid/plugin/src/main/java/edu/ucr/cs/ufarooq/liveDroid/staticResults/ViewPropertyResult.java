package edu.ucr.cs.ufarooq.liveDroid.staticResults;

public class ViewPropertyResult {
    private final String getterSignature;
    private final String propertyName;
    private final String setterSignature;
    private final boolean isToString;

    public ViewPropertyResult(String propertyName, String setterSignature, String getterSignature, boolean isToString) {
        this.getterSignature = getterSignature;
        this.propertyName = propertyName;
        this.setterSignature = setterSignature;
        this.isToString = isToString;
    }

    public String getGetterSignature() {
        return getterSignature;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getSetterSignature() {
        return setterSignature;
    }

    public boolean isToString() {
        return isToString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ViewPropertyResult that = (ViewPropertyResult) o;

        if (isToString != that.isToString) return false;
        if (getterSignature != null ? !getterSignature.equals(that.getterSignature) : that.getterSignature != null)
            return false;
        if (propertyName != null ? !propertyName.equals(that.propertyName) : that.propertyName != null) return false;
        return setterSignature != null ? setterSignature.equals(that.setterSignature) : that.setterSignature == null;
    }

    @Override
    public int hashCode() {
        int result = getterSignature != null ? getterSignature.hashCode() : 0;
        result = 31 * result + (propertyName != null ? propertyName.hashCode() : 0);
        result = 31 * result + (setterSignature != null ? setterSignature.hashCode() : 0);
        result = 31 * result + (isToString ? 1 : 0);
        return result;
    }
}
