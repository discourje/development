package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

class Not implements CtlOperator {
    private final CtlOperator arg;

    Not(CtlOperator args) {
        this.arg = args;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        arg.label(model);

        for (DMState<?> state : model.getStates()) {
            if (!state.hasLabel(arg)) {
                state.addLabel(this);
            }
        }
    }

    public String toString() {
        return "not(" + arg + ")";
    }
}
