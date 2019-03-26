package discourje.demo.javaObjects;

public class Book implements Product {

    private String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public Book(String name) {
        this.name = name;
    }
    public Book() {
    }
}
