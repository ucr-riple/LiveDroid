package edu.ucr.cs.ufarooq.liveDroid.staticResults;

import java.util.HashSet;
import java.util.Set;

public class ViewResult {

    private final int id;
    private final String name;
    private final String viewType;
    private Set<ViewPropertyResult> properties;

    public ViewResult(int id, String name, String viewType) {
        this.id = id;
        this.name = name;
        this.viewType = viewType;
        this.properties = new HashSet<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getViewType() {
        return viewType;
    }

    public Set<ViewPropertyResult> getProperties() {
        return properties;
    }

    public void addProperty(ViewPropertyResult viewPropertyResult) {
        this.properties.add(viewPropertyResult);
    }


    public void setProperties(Set<ViewPropertyResult> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ViewResult that = (ViewResult) o;

        if (id != that.id) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (viewType != null ? !viewType.equals(that.viewType) : that.viewType != null) return false;
        return properties != null ? properties.equals(that.properties) : that.properties == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (viewType != null ? viewType.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }
}
