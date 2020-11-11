package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

public class EF implements CtlOperator {
    private final CtlOperator arg;

    public EF(CtlOperator arg) {
        this.arg = arg;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        CtlOperator au = new EU(True.TRUE, arg);
        au.label(model);
        for (DMState<?> state : model.getStates()) {
            if (state.hasLabel(au)) {
                state.addLabel(this);
            }
        }
    }

    @Override
    public String toString() {
        return "EF(" + arg + ")";
    }
}
