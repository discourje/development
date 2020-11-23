package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

public class EY implements CtlOperator {
    private final CtlOperator arg;

    public EY(CtlOperator arg) {
        this.arg = arg;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        arg.label(model);
        for (DMState<?> state : model.getStates()) {
            if (state.anyPredecessorHasLabel(arg)) {
                state.addLabel(this);
            }
        }
    }

    @Override
    public String toString() {
        return "EY(" + arg + ")";
    }
}
