package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

class True implements CtlOperator {

    @Override
    public void label(DiscourjeModel<?> model) {
        for (DMState<?> state : model.getStates()) {
            state.addLabel(this);
        }
    }

    public String toString() {
        return "true";
    }
}
