package discourje.demo.javaObjects;

public class OrderAcknowledgement {

    private discourje.demo.javaObjects.Order order;
    private String message;

    public OrderAcknowledgement() {
    }

    public discourje.demo.javaObjects.Order getOrder() {
        return order;
    }

    public void setOrder(discourje.demo.javaObjects.Order order) {
        this.order = order;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
