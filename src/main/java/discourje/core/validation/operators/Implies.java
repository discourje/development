package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

public class Implies implements CtlOperator {
    private CtlOperator lhs;
    private CtlOperator rhs;

    public Implies(CtlOperator lhs, CtlOperator rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        lhs.label(model);
        rhs.label(model);

        for (DMState<?> state : model.getStates()) {
            if (!state.hasLabel(lhs) || state.hasLabel(rhs)) {
                state.addLabel(this);
            }
        }
    }

    @Override
    public String toString() {
        return "(" + lhs + " --> " + rhs + ")";
    }
}
