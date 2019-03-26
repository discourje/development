package discourje.demo.javaObjects;

public class Order {

    private discourje.demo.javaObjects.Product product;
    private int quote;

    public Order(Product product, int quote) {
        this.product = product;
        this.quote = quote;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuote() {
        return quote;
    }

}
