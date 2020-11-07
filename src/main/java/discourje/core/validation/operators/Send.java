package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

class Send implements CtlOperator {
    private final String role;

    Send(String role) {
        this.role = role;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        for (DMState<?> state : model.getStates()) {
            Action action = state.getAction();
            if (action.getType() != Action.Type.CLOSE &&
                    role.equals(action.getSender())) {
                state.addLabel(this);
            }
        }
    }
}
