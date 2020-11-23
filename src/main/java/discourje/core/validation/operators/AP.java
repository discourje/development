package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

public class AP implements CtlOperator {
    private final CtlOperator arg;

    public AP(CtlOperator arg) {
        this.arg = arg;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        CtlOperator ap = new AS(True.TRUE, arg);
        ap.label(model);
        for (DMState<?> state : model.getStates()) {
            if (state.hasLabel(ap)) {
                state.addLabel(this);
            }
        }
    }

    @Override
    public String toString() {
        return "AP(" + arg + ")";
    }
}
