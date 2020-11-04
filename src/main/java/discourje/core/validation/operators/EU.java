package discourje.core.validation.operators;

class EU implements CtlOperator {
    private final CtlOperator lhs;
    private final CtlOperator rhs;

    EU(CtlOperator lhs, CtlOperator rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }
}
