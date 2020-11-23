package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

public class EP implements CtlOperator {
    private final CtlOperator arg;

    public EP(CtlOperator arg) {
        this.arg = arg;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        CtlOperator ep = new ES(True.TRUE, arg);
        ep.label(model);
        for (DMState<?> state : model.getStates()) {
            if (state.hasLabel(ep)) {
                state.addLabel(this);
            }
        }
    }

    @Override
    public String toString() {
        return "EP(" + arg + ")";
    }
}
