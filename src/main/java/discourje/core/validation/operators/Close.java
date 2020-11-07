package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

class Close implements CtlOperator {
    private final String role1;
    private final String role2;

    Close(String role1, String role2) {
        this.role1 = role1;
        this.role2 = role2;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        for (DMState<?> state : model.getStates()) {
            Action action = state.getAction();
            if (action != null &&
                    action.getType() == Action.Type.CLOSE &&
                    role1.equals(action.getSender()) &&
                    role2.equals(action.getReceiver())) {
                state.addLabel(this);
            }
        }
    }

    public String toString() {
        return String.format("close_%s_%s", role1, role2);
    }
}
