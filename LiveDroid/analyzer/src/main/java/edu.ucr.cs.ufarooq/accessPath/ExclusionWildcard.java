package edu.ucr.cs.ufarooq.accessPath;

public interface ExclusionWildcard<Location> extends Wildcard {
    public Location excludes();
}