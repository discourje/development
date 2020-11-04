package discourje.core.validation.operators;

class AU implements CtlOperator {
    private final CtlOperator lhs;
    private final CtlOperator rhs;

    AU(CtlOperator lhs, CtlOperator rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }
}
