package discourje.core.validation.operators;

class Not implements CtlOperator {

    private final CtlOperator args;

    Not(CtlOperator args) {
        this.args = args;
    }
}
