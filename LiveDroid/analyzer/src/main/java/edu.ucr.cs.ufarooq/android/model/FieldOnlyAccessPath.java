package edu.ucr.cs.ufarooq.android.model;


import edu.ucr.cs.ufarooq.accessPath.Field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class FieldOnlyAccessPath {
    private final List<Field> fieldChain;

    public FieldOnlyAccessPath(Collection<Field> fieldChain) {
        this.fieldChain = new ArrayList<>(fieldChain);
    }

    public List<Field> getFields() {
        return fieldChain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldOnlyAccessPath that = (FieldOnlyAccessPath) o;
        return Objects.equals(fieldChain, that.fieldChain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldChain);
    }

    @Override
    public String toString() {
        return (fieldChain.isEmpty() ? "" : fieldChain.toString());
    }
}
