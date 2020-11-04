package discourje.core.validation.operators;

class Or implements CtlOperator {

    private final CtlOperator[] args;

    Or(CtlOperator... args) {
        this.args = args;
    }
}
