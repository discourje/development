package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

class Self implements CtlOperator {
    private final String role;

    Self(String role) {
        this.role = role;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        for (DMState<?> state : model.getStates()) {
            Action action = state.getAction();
            if (action != null &&
                    role.equals(action.getSender()) &&
                    role.equals(action.getReceiver())) {
                state.addLabel(this);
            }
        }
    }
}
