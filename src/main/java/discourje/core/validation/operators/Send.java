package discourje.core.validation.operators;

class Send implements CtlOperator {
    private final String role;

    Send(String role) {
        this.role = role;
    }
}
