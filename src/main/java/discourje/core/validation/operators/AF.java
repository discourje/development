package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

public class AF implements CtlOperator {
    private final CtlOperator arg;

    public AF(CtlOperator arg) {
        this.arg = arg;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        CtlOperator au = new AU(new True(), arg);
        au.label(model);
        for (DMState<?> state : model.getStates()) {
            if (state.hasLabel(au)) {
                state.addLabel(this);
            }
        }
    }

    @Override
    public String toString() {
        return "AF(" + arg + ")";
    }
}
