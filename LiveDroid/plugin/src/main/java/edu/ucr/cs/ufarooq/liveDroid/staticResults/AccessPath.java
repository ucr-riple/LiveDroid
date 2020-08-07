package edu.ucr.cs.ufarooq.liveDroid.staticResults;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AccessPath {
    private final List<PsiField> fieldChain;

    public AccessPath() {
        this.fieldChain = Lists.newArrayList();
    }

    public AccessPath(PsiField field) {
        this.fieldChain = Lists.newArrayList(field);
    }

    public AccessPath(List<PsiField> fields) {
        this.fieldChain = fields;
    }

    @Override
    public String toString() {
        return (fieldChain.isEmpty() ? "" : fieldChain.toString());
    }


    public List<PsiField> getFields() {
        return fieldChain;
    }

    public boolean isSingleField() {
        return (!fieldChain.isEmpty()) && fieldChain.size() == 1;
    }

    public String getKey() {
        StringBuilder stringBuilder = new StringBuilder();
        for (PsiField field : fieldChain) {
            stringBuilder.append(field.getName() + "#");
        }
        return stringBuilder.toString();
    }

    public AccessPath getSaveRestoreablePath(AccessPath accessPath) {
        List<PsiField> newList = new ArrayList<>();
        boolean classNotWritable = false;
        for (int i = 0; i < accessPath.fieldChain.size(); i++) {
            PsiField current = accessPath.fieldChain.get(i);
            if (!classNotWritable && current != null && current.getContainingClass().isWritable()) {
                newList.add(i, current);
            } else {
                classNotWritable = true;
                break;
            }
        }
        return new AccessPath(newList);
    }

    public String getVariableName() {
        StringBuilder stringBuilder = new StringBuilder();
        for (PsiField field : fieldChain) {
            stringBuilder.append(field.getName());
        }
        return stringBuilder.toString();
    }

    public String getVariableNameUntil(PsiField psiField) {
        int index = this.fieldChain.indexOf(psiField);
        List<PsiField> subList = this.fieldChain.subList(0, index + 1);
        StringBuilder stringBuilder = new StringBuilder();
        for (PsiField field : subList) {
            stringBuilder.append(field.getName());
        }
        return stringBuilder.toString();
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fieldChain == null) ? 0 : fieldChain.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AccessPath other = (AccessPath) obj;
        if (fieldChain == null) {
            if (other.fieldChain != null) {
                return false;
            }
        } else if (!fieldChain.equals(other.fieldChain)) {
            return false;
        }
        return true;
    }

}
