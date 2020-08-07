package edu.ucr.cs.ufarooq.accessPath;

import com.google.common.collect.Lists;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import edu.ucr.cs.ufarooq.config.Configuration;
import soot.Local;

import java.util.List;
import java.util.Set;


public class AccessPath {
    private final Val val;
    private final List<Field> fieldChain;

    public AccessPath(Val value) {
        this.val = value;
        this.fieldChain = Lists.newArrayList();
    }

    public AccessPath(Val value, Field field) {
        this.val = value;
        this.fieldChain = Lists.newArrayList(field);
    }

    public AccessPath(Val value, List<Field> fields) {
        this.val = value;
        if (fields.size() > Configuration.v().getAccessPathSize()) {
            fields = fields.subList(0, Configuration.v().getAccessPathSize());
        }

        this.fieldChain = fields;

    }

    @Override
    public String toString() {
        return val.value().toString() + "" + (fieldChain.isEmpty() ? "" : fieldChain.toString())
                + (isOverApproximated() ? "*" : "");
    }

    public boolean isOverApproximated() {
        return fieldChain instanceof Set;
    }

    public Val getBase() {
        return this.val;
    }

    public List<Field> getFields() {
        return fieldChain;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fieldChain == null) ? 0 : fieldChain.hashCode());
        result = prime * result + ((val == null) ? 0 : val.hashCode());
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
        if (val == null) {
            if (other.val != null) {
                return false;
            }
        } else if (!val.equals(other.val)) {
            return false;
        }
        return true;
    }

}