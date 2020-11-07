package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

class First implements CtlOperator {

    @Override
    public void label(DiscourjeModel<?> model) {
        for (DMState<?> state : model.getStates()) {
            Action action = state.getAction();
            if (action == null) {
                state.addLabel(this);
            }
        }
    }

}
