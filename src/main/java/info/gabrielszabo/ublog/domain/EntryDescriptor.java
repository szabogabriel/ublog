package info.gabrielszabo.ublog.domain;

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

}
