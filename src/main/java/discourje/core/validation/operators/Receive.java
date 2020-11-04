package discourje.core.validation.operators;

class Receive implements CtlOperator {
    private final String role;

    Receive(String role) {
        this.role = role;
    }
}
