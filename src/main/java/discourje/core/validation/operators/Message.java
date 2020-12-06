package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.Objects;

class Message implements CtlOperator {
    private final String role1;
    private final String role2;
    private final int hash;

    Message(String role1, String role2) {
        this.role1 = role1;
        this.role2 = role2;
        hash = Objects.hash(this.role1, this.role2);
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            for (DMState<?> state : model.getStates()) {
                Action action = state.getAction();
                if (action != null &&
                        action.getType() != Action.Type.CLOSE &&
                        role1.equals(action.getSender()) &&
                        role2.equals(action.getReceiver())) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    public String toString() {
        return String.format("msg_%s_%s", role1, role2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message that = (Message) o;
        return role1.equals(that.role1) &&
                role2.equals(that.role2);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
