package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.Objects;

class Self implements CtlOperator {
    private final String role;
    private final int hash;

    Self(String role) {
        this.role = role;
        hash = Objects.hash(this.role);
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            for (DMState<?> state : model.getStates()) {
                Action action = state.getAction();
                if (action != null &&
                        role.equals(action.getSender()) &&
                        role.equals(action.getReceiver())) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public String toString() {
        return String.format("self_%s", role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Self that = (Self) o;
        return role.equals(that.role);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
