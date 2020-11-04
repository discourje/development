package discourje.core.validation.operators;

class And implements CtlOperator {

    private final CtlOperator[] args;

    And(CtlOperator... args) {
        this.args = args;
    }
}
