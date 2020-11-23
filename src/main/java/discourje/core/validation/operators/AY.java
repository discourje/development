package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

public class AY implements CtlOperator {
    private final CtlOperator arg;

    public AY(CtlOperator arg) {
        this.arg = arg;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        arg.label(model);
        for (DMState<?> state : model.getStates()) {
            if (state.predecessorsExistAndAllHaveLabel(arg)) {
                state.addLabel(this);
            }
        }
    }

    @Override
    public String toString() {
        return "AY(" + arg + ")";
    }
}
