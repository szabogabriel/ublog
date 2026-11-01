package info.gabrielszabo.ublog.domain;

import java.util.Objects;

public class EntryDescriptor {
    private String name;
    private Category category;
    
    public EntryDescriptor() {

    }

    public EntryDescriptor(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntryDescriptor that = (EntryDescriptor) o;
        return Objects.equals(name, that.name) && Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, category);
    }

}
