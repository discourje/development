package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

public class AX implements CtlOperator {
    private final CtlOperator arg;

    public AX(CtlOperator arg) {
        this.arg = arg;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        arg.label(model);
        for (DMState<?> state : model.getStates()) {
            if (state.successorsExistAndAllHaveLabel(arg)) {
                state.addLabel(this);
            }
        }
    }

    @Override
    public String toString() {
        return "AX(" + arg + ")";
    }
}
