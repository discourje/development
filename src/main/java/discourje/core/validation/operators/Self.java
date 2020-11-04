package discourje.core.validation.operators;

class Self implements CtlOperator {
    private final String role;

    Self(String role) {
        this.role = role;
    }
}
