package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

class Receive implements CtlOperator {
    private final String role;

    Receive(String role) {
        this.role = role;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        for (DMState<?> state : model.getStates()) {
            Action action = state.getAction();
            if (action != null &&
                    action.getType() != Action.Type.CLOSE &&
                    role.equals(action.getReceiver())) {
                state.addLabel(this);
            }
        }
    }

    public String toString() {
        return "rcv_" + role;
    }
}
