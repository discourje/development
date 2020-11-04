package discourje.core.validation.operators;

class Close implements CtlOperator {
    private final String role1;
    private final String role2;

    Close(String role1, String role2) {
        this.role1 = role1;
        this.role2 = role2;
    }
}
